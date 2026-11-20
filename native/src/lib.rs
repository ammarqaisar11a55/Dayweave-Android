//! The JNI boundary. Every call crosses as JSON through the same `rpc` dispatcher the desktop uses, so
//! this file only opens the core, forwards calls and turns failures into JSON the app can read.

use std::panic::{catch_unwind, AssertUnwindSafe};
use std::path::Path;
use std::sync::OnceLock;

use dayweave_core::Core;
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use jni::JNIEnv;
use serde_json::{json, Value};

static CORE: OnceLock<Core> = OnceLock::new();

/// Opens the database in `data_dir`. Opening again is harmless: the first core stays in use.
pub fn open(data_dir: &str) -> Result<(), String> {
    if CORE.get().is_some() {
        return Ok(());
    }
    let core = Core::open(Path::new(data_dir)).map_err(|error| error.to_string())?;
    let _ = CORE.set(core);
    Ok(())
}

fn envelope(result: Result<Value, Value>) -> String {
    match result {
        Ok(value) => json!({ "ok": value }),
        Err(error) => json!({ "error": error }),
    }
    .to_string()
}

fn failure(code: &str, message: impl Into<String>) -> Value {
    json!({ "code": code, "message": message.into() })
}

fn with_core(run: impl FnOnce(&Core) -> Result<Value, Value>) -> String {
    let outcome = catch_unwind(AssertUnwindSafe(|| match CORE.get() {
        Some(core) => run(core),
        None => Err(failure("not_open", "The database isn't open yet")),
    }));
    envelope(outcome.unwrap_or_else(|_| Err(failure("internal", "Something went wrong inside Dayweave"))))
}

/// Runs one operation. Returns `{"ok": result}` or `{"error": {"code", "message"}}`.
pub fn rpc(method: &str, params: &str) -> String {
    with_core(|core| {
        let params: Value = if params.trim().is_empty() {
            json!({})
        } else {
            serde_json::from_str(params)
                .map_err(|error| failure("invalid", format!("Invalid request: {error}")))?
        };
        core.rpc(method, params).map_err(|error| json!(error))
    })
}

/// One scheduler pass. Returns the notices to show, in the same envelope as `rpc`.
pub fn tick() -> String {
    with_core(|core| core.tick().map(|notices| json!(notices)).map_err(|error| json!(error)))
}

fn read(env: &mut JNIEnv, text: &JString) -> String {
    env.get_string(text).map(Into::into).unwrap_or_default()
}

fn reply(env: &mut JNIEnv, text: String) -> jstring {
    env.new_string(text).map(|value| value.into_raw()).unwrap_or(std::ptr::null_mut())
}

/// Returns null on success, or a message describing why the database couldn't be opened.
#[no_mangle]
pub extern "system" fn Java_io_github_ammarqaisar11a55_dayweave_core_NativeCore_open(
    mut env: JNIEnv,
    _class: JClass,
    data_dir: JString,
) -> jstring {
    let dir = read(&mut env, &data_dir);
    match catch_unwind(|| open(&dir)) {
        Ok(Ok(())) => std::ptr::null_mut(),
        Ok(Err(message)) => reply(&mut env, message),
        Err(_) => reply(&mut env, "Dayweave could not start".into()),
    }
}

#[no_mangle]
pub extern "system" fn Java_io_github_ammarqaisar11a55_dayweave_core_NativeCore_rpc(
    mut env: JNIEnv,
    _class: JClass,
    method: JString,
    params: JString,
) -> jstring {
    let method = read(&mut env, &method);
    let params = read(&mut env, &params);
    let result = rpc(&method, &params);
    reply(&mut env, result)
}

#[no_mangle]
pub extern "system" fn Java_io_github_ammarqaisar11a55_dayweave_core_NativeCore_tick(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let result = tick();
    reply(&mut env, result)
}

#[cfg(test)]
mod tests {
    use super::*;

    // The core is process-wide, so one test covers the whole sequence.
    #[test]
    fn calls_cross_the_boundary_as_json_envelopes() {
        assert!(rpc("app.info", "{}").contains("not_open"));

        let dir = tempfile::tempdir().unwrap();
        open(dir.path().to_str().unwrap()).unwrap();
        open(dir.path().to_str().unwrap()).expect("opening twice is harmless");

        let created: Value =
            serde_json::from_str(&rpc("tasks.create", r#"{"title":"From Android"}"#)).unwrap();
        assert_eq!(created["ok"]["title"], "From Android");

        let empty: Value = serde_json::from_str(&rpc("overview.get", "")).unwrap();
        assert!(empty["ok"]["date"].is_string(), "an empty body means no parameters");

        let invalid: Value = serde_json::from_str(&rpc("tasks.create", r#"{"title":"  "}"#)).unwrap();
        assert_eq!(invalid["error"]["code"], "invalid");

        let garbage: Value = serde_json::from_str(&rpc("tasks.list", "{not json")).unwrap();
        assert_eq!(garbage["error"]["code"], "invalid");

        let unknown: Value = serde_json::from_str(&rpc("tasks.explode", "{}")).unwrap();
        assert_eq!(unknown["error"]["code"], "unknown_method");

        let ticked: Value = serde_json::from_str(&tick()).unwrap();
        assert!(ticked["ok"].is_array());

        let wake: Value = serde_json::from_str(&rpc("scheduler.nextWake", "{}")).unwrap();
        assert!(wake["ok"]["at"].is_null());
    }
}
