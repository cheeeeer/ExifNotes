/*
 * Exif Notes
 * Copyright (C) 2022  Tommi Hirvonen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tommihirvonen.exifnotes.fragments

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.transition.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.FragmentCameraEditBinding
import com.tommihirvonen.exifnotes.datastructures.Camera
import com.tommihirvonen.exifnotes.datastructures.Increment
import com.tommihirvonen.exifnotes.datastructures.Lens
import com.tommihirvonen.exifnotes.datastructures.PartialIncrement
import com.tommihirvonen.exifnotes.utilities.*

/**
 * Dialog to edit Camera's information
 */
class CameraEditFragment : Fragment() {

    private lateinit var binding: FragmentCameraEditBinding

    private val camera by lazy { requireArguments().getParcelable(ExtraKeys.CAMERA) ?: Camera() }

    private val newCamera by lazy { camera.copy() }

    /**
     * Stores the currently displayed shutter speed values.
     * Changes depending on the currently selected shutter increments
     */
    private lateinit var displayedShutterValues: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            navigateBack()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentCameraEditBinding.inflate(inflater)

        val transitionName = requireArguments().getString(ExtraKeys.TRANSITION_NAME)
        binding.root.transitionName = transitionName
        binding.topAppBar.title = requireArguments().getString(ExtraKeys.TITLE)

        // EDIT TEXT FIELDS
        binding.makeEditText.setText(camera.make)
        binding.modelEditText.setText(camera.model)
        binding.serialNumberEditText.setText(camera.serialNumber)

        // SHUTTER SPEED INCREMENTS
        try {
            binding.shutterSpeedIncrementSpinner.setSelection(newCamera.shutterIncrements.ordinal)
        } catch (e: ArrayIndexOutOfBoundsException) {
            e.printStackTrace()
        }
        binding.shutterSpeedIncrementSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                //Shutter speed increments were changed, make update
                //Check if the new increments include both min and max values.
                //Otherwise reset them to null
                var minFound = false
                var maxFound = false
                displayedShutterValues = when (newCamera.shutterIncrements) {
                    Increment.THIRD -> requireActivity().resources.getStringArray(R.array.ShutterValuesThird)
                    Increment.HALF -> requireActivity().resources.getStringArray(R.array.ShutterValuesHalf)
                    Increment.FULL -> requireActivity().resources.getStringArray(R.array.ShutterValuesFull)
                }
                for (string in displayedShutterValues) {
                    if (!minFound && string == newCamera.minShutter) minFound = true
                    if (!maxFound && string == newCamera.maxShutter) maxFound = true
                    if (minFound && maxFound) break
                }
                //If either one wasn't found in the new values array, null them.
                if (!minFound || !maxFound) {
                    newCamera.minShutter = null
                    newCamera.maxShutter = null
                    updateShutterRangeTextView()
                }
            }
        }

        // SHUTTER RANGE BUTTON
        updateShutterRangeTextView()
        binding.shutterRangeLayout.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(requireActivity())
            val dialogView = inflater.inflate(R.layout.dialog_double_numberpicker, null)
            val minShutterPicker = dialogView.findViewById<NumberPicker>(R.id.number_picker_one)
            val maxShutterPicker = dialogView.findViewById<NumberPicker>(R.id.number_picker_two)

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
                    binding.root.snackbar(R.string.NoMinOrMaxShutter)
                } else {
                    if (minShutterPicker.value == displayedShutterValues.size - 1 &&
                            maxShutterPicker.value == displayedShutterValues.size - 1) {
                        newCamera.minShutter = null
                        newCamera.maxShutter = null
                    } else if (minShutterPicker.value < maxShutterPicker.value) {
                        newCamera.minShutter = displayedShutterValues[minShutterPicker.value]
                        newCamera.maxShutter = displayedShutterValues[maxShutterPicker.value]
                    } else {
                        newCamera.minShutter = displayedShutterValues[maxShutterPicker.value]
                        newCamera.maxShutter = displayedShutterValues[minShutterPicker.value]
                    }
                    updateShutterRangeTextView()
                }
                dialog.dismiss()
            }
        }


        // EXPOSURE COMPENSATION INCREMENTS
        try {
            binding.exposureCompIncrementSpinner.setSelection(newCamera.exposureCompIncrements.ordinal)
        } catch (e: ArrayIndexOutOfBoundsException) {
            e.printStackTrace()
        }

        // FIXED LENS
        binding.fixedLensHelp.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext()).apply {
                setMessage(R.string.FixedLensHelp)
                setPositiveButton(R.string.Close) { _: DialogInterface, _: Int -> }
            }.create().show()
        }
        newCamera.lens?.let {
            binding.fixedLensText.text = resources.getString(R.string.ClickToEdit)
            binding.lensClear.visibility = View.VISIBLE
        } ?: run {
            binding.fixedLensText.text = resources.getString(R.string.ClickToSet)
            binding.lensClear.visibility = View.GONE
        }
        binding.fixedLensLayout.setOnClickListener {
            showFixedLensFragment()
        }
        binding.lensClear.setOnClickListener {
            newCamera.lens = null
            binding.fixedLensText.text = resources.getString(R.string.ClickToSet)
            binding.lensClear.visibility = View.GONE
        }

        binding.topAppBar.setNavigationOnClickListener { navigateBack() }

        binding.positiveButton.setOnClickListener {
            val make = binding.makeEditText.text.toString()
            val model = binding.modelEditText.text.toString()
            val serialNumber = binding.serialNumberEditText.text.toString()
            if (make.isEmpty() && model.isEmpty()) {
                // No make or model was set
                binding.root.snackbar(R.string.NoMakeOrModel)
            } else if (make.isNotEmpty() && model.isEmpty()) {
                // No model was set
                binding.root.snackbar(R.string.NoModel)
            } else if (make.isEmpty()) {
                // No make was set
                binding.root.snackbar(R.string.NoMake)
            } else {

                camera.make = make
                camera.model = model
                camera.serialNumber = serialNumber
                camera.shutterIncrements = Increment.from(binding.shutterSpeedIncrementSpinner.selectedItemPosition)
                camera.minShutter = newCamera.minShutter
                camera.maxShutter = newCamera.maxShutter
                camera.exposureCompIncrements = PartialIncrement.from(binding.exposureCompIncrementSpinner.selectedItemPosition)
                camera.lens = newCamera.lens

                val bundle = Bundle()
                bundle.putParcelable(ExtraKeys.CAMERA, camera)
                setFragmentResult("CameraEditFragment", bundle)
                navigateBack()
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        // Start the transition once all views have been measured and laid out.
        (view.parent as ViewGroup).doOnPreDraw {
            startPostponedEnterTransition()
        }
    }

    private fun navigateBack() =
        requireParentFragment().childFragmentManager.popBackStack()

    private fun showFixedLensFragment() {
        val sharedElementTransition = TransitionSet()
            .addTransition(ChangeBounds())
            .addTransition(ChangeTransform())
            .addTransition(ChangeImageTransform())
            .addTransition(Fade())
            .setCommonInterpolator(FastOutSlowInInterpolator())
            .apply { duration = 250L }
        val fragment = LensEditFragment().apply {
            sharedElementEnterTransition = sharedElementTransition
        }
        val arguments = Bundle()
        arguments.putBoolean(ExtraKeys.FIXED_LENS, true)
        newCamera.lens?.let {
            arguments.putParcelable(ExtraKeys.LENS, it)
        }
        arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.SetFixedLens))
        val sharedElement = binding.fixedLensLayout
        arguments.putString(ExtraKeys.TRANSITION_NAME, sharedElement.transitionName)
        fragment.arguments = arguments

        requireParentFragment().childFragmentManager
            .beginTransaction()
            .setReorderingAllowed(true)
            .addSharedElement(sharedElement, sharedElement.transitionName)
            .replace(R.id.gear_fragment_container, fragment)
            .addToBackStack(null)
            .commit()

        fragment.setFragmentResultListener("LensEditFragment") { _, bundle ->
            val lens: Lens = bundle.getParcelable(ExtraKeys.LENS)
                ?: return@setFragmentResultListener
            newCamera.lens = lens
            binding.fixedLensText.text = resources.getString(R.string.ClickToEdit)
            binding.lensClear.visibility = View.VISIBLE
        }
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
        displayedShutterValues = when (newCamera.shutterIncrements) {
            Increment.THIRD -> requireActivity().resources.getStringArray(R.array.ShutterValuesThird)
            Increment.HALF -> requireActivity().resources.getStringArray(R.array.ShutterValuesHalf)
            Increment.FULL -> requireActivity().resources.getStringArray(R.array.ShutterValuesFull)
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
        val initialMinValue = displayedShutterValues.indexOfFirst { it == newCamera.minShutter }
        if (initialMinValue != -1) minShutterPicker.value = initialMinValue
        val initialMaxValue = displayedShutterValues.indexOfFirst { it == newCamera.maxShutter }
        if (initialMaxValue != -1) maxShutterPicker.value = initialMaxValue
    }

    /**
     * Update the shutter speed range Button to display the currently selected shutter speed range.
     */
    private fun updateShutterRangeTextView() {
        binding.shutterRangeText.text =
                if (newCamera.minShutter == null || newCamera.maxShutter == null) resources.getString(R.string.ClickToSet)
                else "${newCamera.minShutter} - ${newCamera.maxShutter}"
    }

}