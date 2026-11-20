package io.github.ammarqaisar11a55.dayweave.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

/** The desktop app's icon names, mapped to their closest Material icon so data moves between them. */
val HabitIcons: Map<String, ImageVector> = linkedMapOf(
    "sparkles" to Icons.Filled.AutoAwesome,
    "target" to Icons.Filled.TrackChanges,
    "book" to Icons.AutoMirrored.Filled.MenuBook,
    "book-open" to Icons.Filled.AutoStories,
    "brain" to Icons.Filled.Psychology,
    "code" to Icons.Filled.Code,
    "graduation-cap" to Icons.Filled.School,
    "languages" to Icons.Filled.Translate,
    "pencil" to Icons.Filled.Edit,
    "dumbbell" to Icons.Filled.FitnessCenter,
    "footprints" to Icons.AutoMirrored.Filled.DirectionsWalk,
    "bike" to Icons.AutoMirrored.Filled.DirectionsBike,
    "heart" to Icons.Filled.Favorite,
    "droplets" to Icons.Filled.WaterDrop,
    "salad" to Icons.Filled.LocalDining,
    "coffee" to Icons.Filled.Coffee,
    "bed" to Icons.Filled.Bed,
    "moon" to Icons.Filled.Bedtime,
    "sun" to Icons.Filled.WbSunny,
    "wind" to Icons.Filled.Air,
    "leaf" to Icons.Filled.Eco,
    "flower" to Icons.Filled.LocalFlorist,
    "flame" to Icons.Filled.LocalFireDepartment,
    "music" to Icons.Filled.MusicNote,
    "palette" to Icons.Filled.Palette,
    "gamepad" to Icons.Filled.SportsEsports,
    "home" to Icons.Filled.Home,
    "briefcase" to Icons.Filled.Work,
    "folder" to Icons.Filled.Folder,
    "rocket" to Icons.Filled.RocketLaunch,
    "star" to Icons.Filled.Star,
    "piggy-bank" to Icons.Filled.Savings,
)

fun iconFor(name: String?): ImageVector = HabitIcons[name] ?: Icons.Filled.AutoAwesome
