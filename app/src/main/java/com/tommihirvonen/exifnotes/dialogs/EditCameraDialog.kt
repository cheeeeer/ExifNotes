package com.tommihirvonen.exifnotes.dialogs

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.DialogFragment
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.datastructures.Camera
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.Utilities
import com.tommihirvonen.exifnotes.utilities.Utilities.ScrollIndicatorNestedScrollViewListener

/**
 * Dialog to edit Camera's information
 */
class EditCameraDialog : DialogFragment() {

    companion object {
        /**
         * Public constant used to tag the fragment when created
         */
        const val TAG = "EditCameraDialog"
    }

    /**
     * Stores the currently selected shutter speed value increment setting
     */
    private var newShutterIncrements = 0

    /**
     * Stores the currently selected exposure compensation increment setting
     */
    private var newExposureCompIncrements = 0

    /**
     * Stores the currently displayed shutter speed values.
     * Changes depending on the currently selected shutter increments
     */
    private lateinit var displayedShutterValues: Array<String>

    /**
     * Reference to the TextView to display the shutter speed range
     */
    private lateinit var shutterRangeTextView: TextView

    /**
     * Currently selected minimum shutter speed (shortest duration)
     */
    private var newMinShutter: String? = null

    /**
     * Currently selected maximum shutter speed (longest duration)
     */
    private var newMaxShutter: String? = null

    override fun onCreateDialog(SavedInstanceState: Bundle?): Dialog {

        // Inflate the fragment. Get the edited camera.
        // Initialize the UI objects and display the camera's information.
        // Add listeners to Buttons to open new dialogs to change the camera's settings.

        val layoutInflater = requireActivity().layoutInflater

        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        @SuppressLint("InflateParams")
        val inflatedView = layoutInflater.inflate(R.layout.dialog_camera, null)

        val alert = AlertDialog.Builder(activity)
        val title = requireArguments().getString(ExtraKeys.TITLE)
        val positiveButton = requireArguments().getString(ExtraKeys.POSITIVE_BUTTON)
        val camera = requireArguments().getParcelable(ExtraKeys.CAMERA) ?: Camera()
        newMinShutter = camera.minShutter
        newMaxShutter = camera.maxShutter

        val nestedScrollView: NestedScrollView = inflatedView.findViewById(R.id.nested_scroll_view)
        nestedScrollView.setOnScrollChangeListener(
                ScrollIndicatorNestedScrollViewListener(
                        requireActivity(),
                        nestedScrollView,
                        inflatedView.findViewById(R.id.scrollIndicatorUp),
                        inflatedView.findViewById(R.id.scrollIndicatorDown)))

        alert.setCustomTitle(Utilities.buildCustomDialogTitleTextView(activity, title))
        alert.setView(inflatedView)

        // Color the dividers white if the app's theme is dark
        if (Utilities.isAppThemeDark(requireActivity().applicationContext)) {
            arrayOf<View>(
                    inflatedView.findViewById(R.id.divider_view1),
                    inflatedView.findViewById(R.id.divider_view2),
                    inflatedView.findViewById(R.id.divider_view3),
                    inflatedView.findViewById(R.id.divider_view4)
            ).forEach { it.setBackgroundColor(ContextCompat.getColor(requireActivity(), R.color.white)) }
        }

        // EDIT TEXT FIELDS
        val makeEditText = inflatedView.findViewById<EditText>(R.id.make_editText)
        makeEditText.setText(camera.make)
        val modelEditText = inflatedView.findViewById<EditText>(R.id.model_editText)
        modelEditText.setText(camera.model)
        val serialNumberEditText = inflatedView.findViewById<EditText>(R.id.serialNumber_editText)
        serialNumberEditText.setText(camera.serialNumber)

        // SHUTTER SPEED INCREMENTS BUTTON
        newShutterIncrements = camera.getShutterIncrements()
        val shutterSpeedIncrementsTextView = inflatedView.findViewById<TextView>(R.id.increment_text)
        shutterSpeedIncrementsTextView.text = resources.getStringArray(R.array.StopIncrements)[camera.getShutterIncrements()]
        val shutterSpeedIncrementLayout = inflatedView.findViewById<LinearLayout>(R.id.increment_layout)

        shutterSpeedIncrementLayout.setOnClickListener {
            val checkedItem = newShutterIncrements
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(resources.getString(R.string.ChooseIncrements))
            builder.setSingleChoiceItems(R.array.StopIncrements, checkedItem) { dialogInterface: DialogInterface, i: Int ->
                newShutterIncrements = i
                shutterSpeedIncrementsTextView.text = resources.getStringArray(R.array.StopIncrements)[i]

                //Shutter speed increments were changed, make update
                //Check if the new increments include both min and max values.
                //Otherwise reset them to null
                var minFound = false
                var maxFound = false
                displayedShutterValues = when (newShutterIncrements) {
                    1 -> requireActivity().resources.getStringArray(R.array.ShutterValuesHalf)
                    2 -> requireActivity().resources.getStringArray(R.array.ShutterValuesFull)
                    0 -> requireActivity().resources.getStringArray(R.array.ShutterValuesThird)
                    else -> requireActivity().resources.getStringArray(R.array.ShutterValuesThird)
                }
                for (string in displayedShutterValues) {
                    if (!minFound && string == newMinShutter) minFound = true
                    if (!maxFound && string == newMaxShutter) maxFound = true
                    if (minFound && maxFound) break
                }
                //If either one wasn't found in the new values array, null them.
                if (!minFound || !maxFound) {
                    newMinShutter = null
                    newMaxShutter = null
                    updateShutterRangeTextView()
                }
                dialogInterface.dismiss()
            }
            builder.setNegativeButton(resources.getString(R.string.Cancel)) { _: DialogInterface?, _: Int -> }
            builder.create().show()
        }

        // SHUTTER RANGE BUTTON
        shutterRangeTextView = inflatedView.findViewById(R.id.shutter_range_text)
        updateShutterRangeTextView()
        val shutterRangeLayout = inflatedView.findViewById<LinearLayout>(R.id.shutter_range_layout)
        shutterRangeLayout.setOnClickListener {
            val builder = AlertDialog.Builder(activity)
            val inflater = requireActivity().layoutInflater
            @SuppressLint("InflateParams")
            val dialogView = inflater.inflate(R.layout.dialog_double_numberpicker, null)
            val minShutterPicker = dialogView.findViewById<NumberPicker>(R.id.number_picker_one)
            val maxShutterPicker = dialogView.findViewById<NumberPicker>(R.id.number_picker_two)
            val color =
                    if (Utilities.isAppThemeDark(requireActivity().applicationContext)) ContextCompat.getColor(requireActivity(), R.color.light_grey)
                    else ContextCompat.getColor(requireActivity(), R.color.grey)
            val dash = dialogView.findViewById<ImageView>(R.id.dash)
            Utilities.setColorFilter(dash.drawable.mutate(), color)

            // To prevent text edit
            minShutterPicker.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            maxShutterPicker.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            initialiseShutterRangePickers(minShutterPicker, maxShutterPicker)
            builder.setView(dialogView)
            builder.setTitle(resources.getString(R.string.ChooseShutterRange))
            builder.setPositiveButton(resources.getString(R.string.OK), null)
            builder.setNegativeButton(resources.getString(R.string.Cancel)) { _: DialogInterface?, _: Int -> }
            val dialog = builder.create()
            dialog.show()
            // Override the positiveButton to check the range before accepting.
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (minShutterPicker.value == displayedShutterValues.size - 1 &&
                        maxShutterPicker.value != displayedShutterValues.size - 1
                        ||
                        minShutterPicker.value != displayedShutterValues.size - 1 &&
                        maxShutterPicker.value == displayedShutterValues.size - 1) {
                    // No min or max shutter was set
                    Toast.makeText(activity, resources.getString(R.string.NoMinOrMaxShutter),
                            Toast.LENGTH_LONG).show()
                } else {
                    if (minShutterPicker.value == displayedShutterValues.size - 1 &&
                            maxShutterPicker.value == displayedShutterValues.size - 1) {
                        newMinShutter = null
                        newMaxShutter = null
                    } else if (minShutterPicker.value < maxShutterPicker.value) {
                        newMinShutter = displayedShutterValues[minShutterPicker.value]
                        newMaxShutter = displayedShutterValues[maxShutterPicker.value]
                    } else {
                        newMinShutter = displayedShutterValues[maxShutterPicker.value]
                        newMaxShutter = displayedShutterValues[minShutterPicker.value]
                    }
                    updateShutterRangeTextView()
                }
                dialog.dismiss()
            }
        }


        // EXPOSURE COMPENSATION INCREMENTS BUTTON
        newExposureCompIncrements = camera.getExposureCompIncrements()
        val exposureCompIncrementsTextView = inflatedView.findViewById<TextView>(R.id.exposure_comp_increment_text)
        exposureCompIncrementsTextView.text = resources.getStringArray(R.array.ExposureCompIncrements)[camera.getExposureCompIncrements()]
        val exposureCompIncrementLayout = inflatedView.findViewById<LinearLayout>(R.id.exposure_comp_increment_layout)
        exposureCompIncrementLayout.setOnClickListener {
            val checkedItem = newExposureCompIncrements
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(resources.getString(R.string.ChooseIncrements))
            builder.setSingleChoiceItems(R.array.ExposureCompIncrements, checkedItem) { dialogInterface: DialogInterface, i: Int ->
                newExposureCompIncrements = i
                exposureCompIncrementsTextView.text = resources.getStringArray(R.array.ExposureCompIncrements)[i]
                dialogInterface.dismiss()
            }
            builder.setNegativeButton(resources.getString(R.string.Cancel)) { _: DialogInterface?, _: Int -> }
            builder.create().show()
        }


        // FINALISE BUILDING THE DIALOG
        alert.setPositiveButton(positiveButton, null)
        alert.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int ->
            val intent = Intent()
            targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_CANCELED, intent)
        }
        val dialog = alert.create()

        // SOFT_INPUT_ADJUST_PAN: set to have a window pan when an input method is shown,
        // so it doesn't need to deal with resizing
        // but just panned by the framework to ensure the current input focus is visible
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        dialog.show()

        // We override the positive button onClick so that we can dismiss the dialog
        // only when both make and model are set.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val make = makeEditText.text.toString()
            val model = modelEditText.text.toString()
            val serialNumber = serialNumberEditText.text.toString()
            if (make.isEmpty() && model.isEmpty()) {
                // No make or model was set
                Toast.makeText(activity, resources.getString(R.string.NoMakeOrModel), Toast.LENGTH_SHORT).show()
            } else if (make.isNotEmpty() && model.isEmpty()) {
                // No model was set
                Toast.makeText(activity, resources.getString(R.string.NoModel), Toast.LENGTH_SHORT).show()
            } else if (make.isEmpty()) {
                // No make was set
                Toast.makeText(activity, resources.getString(R.string.NoMake), Toast.LENGTH_SHORT).show()
            } else {
                camera.make = make
                camera.model = model
                camera.serialNumber = serialNumber
                camera.setShutterIncrements(newShutterIncrements)
                camera.minShutter = newMinShutter
                camera.maxShutter = newMaxShutter
                camera.setExposureCompIncrements(newExposureCompIncrements)

                // Return the new entered name to the calling activity
                val intent = Intent()
                intent.putExtra(ExtraKeys.CAMERA, camera)
                dialog.dismiss()
                targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, intent)
            }
        }
        return dialog
    }

    /**
     * Called when the shutter speed range dialog is opened.
     * Sets the values for the NumberPickers.
     *
     * @param minShutterPicker NumberPicker associated with the minimum shutter speed
     * @param maxShutterPicker NumberPicker associated with the maximum shutter speed
     */
    private fun initialiseShutterRangePickers(minShutterPicker: NumberPicker,
                                              maxShutterPicker: NumberPicker) {
        displayedShutterValues = when (newShutterIncrements) {
            1 -> requireActivity().resources.getStringArray(R.array.ShutterValuesHalf)
            2 -> requireActivity().resources.getStringArray(R.array.ShutterValuesFull)
            0 -> requireActivity().resources.getStringArray(R.array.ShutterValuesThird)
            else -> requireActivity().resources.getStringArray(R.array.ShutterValuesThird)
        }
        if (displayedShutterValues[0] == resources.getString(R.string.NoValue)) {
            displayedShutterValues.reverse()
        }
        minShutterPicker.minValue = 0
        maxShutterPicker.minValue = 0
        minShutterPicker.maxValue = displayedShutterValues.size - 1
        maxShutterPicker.maxValue = displayedShutterValues.size - 1
        minShutterPicker.displayedValues = displayedShutterValues
        maxShutterPicker.displayedValues = displayedShutterValues
        minShutterPicker.value = displayedShutterValues.size - 1
        maxShutterPicker.value = displayedShutterValues.size - 1
        val initialMinValue = displayedShutterValues.indexOfFirst { it == newMinShutter }
        if (initialMinValue != -1) minShutterPicker.value = initialMinValue
        val initialMaxValue = displayedShutterValues.indexOfFirst { it == newMaxShutter }
        if (initialMaxValue != -1) maxShutterPicker.value = initialMaxValue
    }

    /**
     * Update the shutter speed range Button to display the currently selected shutter speed range.
     */
    private fun updateShutterRangeTextView() {
        shutterRangeTextView.text =
                if (newMinShutter == null || newMaxShutter == null) resources.getString(R.string.ClickToSet)
                else "$newMinShutter - $newMaxShutter"
    }

}