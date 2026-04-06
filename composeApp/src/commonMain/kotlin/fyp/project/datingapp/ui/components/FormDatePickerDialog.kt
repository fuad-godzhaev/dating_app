package fyp.project.datingapp.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import datingapp.composeapp.generated.resources.Res
import datingapp.composeapp.generated.resources.cancel
import datingapp.composeapp.generated.resources.done
import datingapp.composeapp.generated.resources.pick_a_date
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormDatePickerDialog(
    visible: Boolean,
    initialDate: LocalDate,
    maxDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toEpochDays() * 86_400_000L,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = LocalDate.fromEpochDays((utcTimeMillis / 86_400_000L).toInt())
                return date <= maxDate
            }
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val selected = LocalDate.fromEpochDays((millis / 86_400_000L).toInt())
                    onDateSelected(selected)
                }
                onDismiss()
            }) {
                Text(stringResource(Res.string.done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    ) {
        DatePicker(
            state = datePickerState,
            title = { Text(stringResource(Res.string.pick_a_date)) },
        )
    }
}