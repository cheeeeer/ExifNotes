package com.tommihirvonen.exifnotes.dialogs

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.NestedScrollView
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.DialogFragment
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.activities.LocationPickActivity
import com.tommihirvonen.exifnotes.datastructures.*
import com.tommihirvonen.exifnotes.datastructures.Filter
import com.tommihirvonen.exifnotes.utilities.*
import com.tommihirvonen.exifnotes.utilities.Utilities.ScrollIndicatorNestedScrollViewListener
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Dialog to edit Frame's information
 */
open class EditFrameDialog : DialogFragment() {

    companion object {
        /**
         * Public constant used to tag the fragment when created
         */
        const val TAG = "EditFrameDialog"

        /**
         * Constant passed to LocationPickActivity for result
         */
        private const val PLACE_PICKER_REQUEST = 1

        /**
         * Constant passed to EditLensDialog for result
         */
        private const val ADD_LENS = 2

        /**
         * Constant passed to EditFilterDialog for result
         */
        private const val ADD_FILTER = 3

        /**
         * Constant passed to takePictureIntent for result
         */
        private const val CAPTURE_IMAGE_REQUEST = 4

        /**
         * Constant passed to selectPictureIntent for result
         */
        private const val SELECT_PICTURE_REQUEST = 5
    }

    private lateinit var roll: Roll

    internal lateinit var frame: Frame

    private lateinit var newFrame: Frame


    /**
     * Holds all the lenses that can be mounted to the used camera
     */
    private var mountableLenses: MutableList<Lens>? = null

    /**
     * Reference to the singleton database
     */
    private lateinit var database: FilmDbHelper

    private lateinit var dateTimeLayoutManager: DateTimeLayoutManager

    /**
     * Button used to display the currently selected location
     */
    private lateinit var locationTextView: TextView

    /**
     * Button used to display the currently selected lens
     */
    private lateinit var lensTextView: TextView

    /**
     * Button used to display the currently selected filter
     */
    private lateinit var filtersTextView: TextView

    /**
     * Reference to the complementary picture's layout. If this view becomes visible,
     * load the complementary picture.
     */
    private lateinit var pictureLayout: LinearLayout

    /**
     * ImageView used to display complementary image taken with the phone's camera
     */
    private lateinit var pictureImageView: ImageView

    /**
     * TextView used to display text related to the complementary picture
     */
    private lateinit var pictureTextView: TextView

    /**
     * Currently selected lens's aperture increment setting
     */
    private var apertureIncrements = 0

    /**
     * The shutter speed increment setting of the camera used
     */
    private var shutterIncrements = 0

    /**
     * The exposure compensation increment setting of the camera used
     */
    private var exposureCompIncrements = 0

    /**
     * Used to temporarily store the possible new picture name. newPictureFilename is only set,
     * if the user presses ok in the camera activity. If the user cancels the camera activity,
     * then this member's value is ignored and newPictureFilename's value isn't changed.
     */
    private var tempPictureFilename: String? = null

    /**
     * TextView used to display the current aperture value
     */
    private lateinit var apertureTextView: TextView

    /**
     * TextView used to display the current shutter speed value
     */
    private lateinit var shutterTextView: TextView

    /**
     * TextView used to display the current frame count
     */
    private lateinit var frameCountTextView: TextView

    /**
     * TextView used to display the current exposure compensation value
     */
    private lateinit var exposureCompTextView: TextView

    /**
     * TextView used to display the current number of exposures value
     */
    private lateinit var noOfExposuresTextView: TextView

    /**
     * Button used to display the current focal length value
     */
    private lateinit var focalLengthTextView: TextView

    /**
     * CheckBox for toggling whether flash was used or not
     */
    private lateinit var flashCheckBox: CheckBox

    /**
     * TextView used to display the current light source
     */
    private lateinit var lightSourceTextView: TextView

    /**
     * Reference to the EditText used to edit notes
     */
    private lateinit var noteEditText: EditText

    /**
     * Stores the currently displayed shutter speed values.
     */
    private lateinit var displayedShutterValues: Array<String>

    /**
     * Stores the currently displayed aperture values.
     * Changes depending on the currently selected lens.
     */
    private lateinit var displayedApertureValues: Array<String>

    /**
     * Stores the currently displayed exposure compensation values.
     * Changes depending on the film's camera.
     */
    private lateinit var displayedExposureCompValues: Array<String>

    override fun onCreateDialog(SavedInstanceState: Bundle?): Dialog {

        // Inflate the fragment. Get the edited frame and used camera.
        // Initialize UI objects and display the frame's information.
        // Add listeners to buttons to open new dialogs to change the frame's information.
        val title = requireArguments().getString(ExtraKeys.TITLE)
        val positiveButton = requireArguments().getString(ExtraKeys.POSITIVE_BUTTON)
        frame = requireArguments().getParcelable(ExtraKeys.FRAME) ?: Frame()
        newFrame = frame.copy()
        database = FilmDbHelper.getInstance(activity)

        roll = database.getRoll(frame.rollId) ?: Roll()
        roll.camera?.let {
            mountableLenses = database.getLinkedLenses(it)
            shutterIncrements = it.shutterIncrements
            exposureCompIncrements = it.exposureCompIncrements
        } ?: run {
            mountableLenses = database.allLenses
        }

        val layoutInflater = requireActivity().layoutInflater
        // Here we can safely pass null, because we are inflating a layout for use in a dialog
        @SuppressLint("InflateParams")
        val inflatedView = layoutInflater.inflate(R.layout.dialog_frame, null)
        val alert = AlertDialog.Builder(activity)
        val nestedScrollView: NestedScrollView = inflatedView.findViewById(R.id.nested_scroll_view)
        val listener = OnScrollChangeListener(
                requireActivity(),
                nestedScrollView,
                inflatedView.findViewById(R.id.scrollIndicatorUp),
                inflatedView.findViewById(R.id.scrollIndicatorDown))
        nestedScrollView.setOnScrollChangeListener(listener)
        alert.setCustomTitle(Utilities.buildCustomDialogTitleTextView(activity, title))
        alert.setView(inflatedView)


        // Color the dividers white if the app's theme is dark
        if (Utilities.isAppThemeDark(activity)) {
            listOf<View>(
                    inflatedView.findViewById(R.id.divider_view1),
                    inflatedView.findViewById(R.id.divider_view2),
                    inflatedView.findViewById(R.id.divider_view3),
                    inflatedView.findViewById(R.id.divider_view4),
                    inflatedView.findViewById(R.id.divider_view5),
                    inflatedView.findViewById(R.id.divider_view6),
                    inflatedView.findViewById(R.id.divider_view7),
                    inflatedView.findViewById(R.id.divider_view8),
                    inflatedView.findViewById(R.id.divider_view9),
                    inflatedView.findViewById(R.id.divider_view10),
                    inflatedView.findViewById(R.id.divider_view11),
                    inflatedView.findViewById(R.id.divider_view12),
                    inflatedView.findViewById(R.id.divider_view13),
                    inflatedView.findViewById(R.id.divider_view14)
            ).forEach { it.setBackgroundColor(ContextCompat.getColor(requireActivity(), R.color.white)) }
            Utilities.setColorFilter((inflatedView.findViewById<ImageView>(R.id.add_lens)).drawable.mutate(),
                    ContextCompat.getColor(requireActivity(), R.color.white))
            Utilities.setColorFilter((inflatedView.findViewById<ImageView>(R.id.add_filter)).drawable.mutate(),
                    ContextCompat.getColor(requireActivity(), R.color.white))
            Utilities.setColorFilter((inflatedView.findViewById<ImageView>(R.id.clear_location)).drawable.mutate(),
                    ContextCompat.getColor(requireActivity(), R.color.white))
        }


        //==========================================================================================
        //LENS TEXT
        lensTextView = inflatedView.findViewById(R.id.lens_text)
        lensTextView.text = frame.lens?.name ?: ""

        // LENS PICK DIALOG
        val lensLayout = inflatedView.findViewById<LinearLayout>(R.id.lens_layout)
        lensLayout.setOnClickListener(LensLayoutOnClickListener())

        // LENS ADD DIALOG
        val addLensImageView = inflatedView.findViewById<ImageView>(R.id.add_lens)
        addLensImageView.isClickable = true
        addLensImageView.setOnClickListener {
            noteEditText.clearFocus()
            val dialog = EditLensDialog()
            dialog.setTargetFragment(this@EditFrameDialog, ADD_LENS)
            val arguments = Bundle()
            arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.NewLens))
            arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.Add))
            dialog.arguments = arguments
            dialog.show(parentFragmentManager.beginTransaction(), EditLensDialog.TAG)
        }


        //==========================================================================================
        // DATE & TIME PICK DIALOG
        val dateLayout = inflatedView.findViewById<LinearLayout>(R.id.date_layout)
        val timeLayout = inflatedView.findViewById<LinearLayout>(R.id.time_layout)
        val dateTextView = inflatedView.findViewById<TextView>(R.id.date_text)
        val timeTextView = inflatedView.findViewById<TextView>(R.id.time_text)
        if (frame.date == null) frame.date = DateTime.fromCurrentTime()
        val dateTime = frame.date
        dateTextView.text = dateTime?.dateAsText
        timeTextView.text = dateTime?.timeAsText
        dateTimeLayoutManager = DateTimeLayoutManager(
                requireActivity(), dateLayout, timeLayout, dateTextView, timeTextView, dateTime, null
        )


        //==========================================================================================
        //NOTES FIELD
        noteEditText = inflatedView.findViewById(R.id.note_editText)
        noteEditText.isSingleLine = false
        noteEditText.setText(frame.note)
        noteEditText.setSelection(noteEditText.text.length)


        //==========================================================================================
        //COUNT BUTTON
        frameCountTextView = inflatedView.findViewById(R.id.frame_count_text)
        frameCountTextView.text = newFrame.count.toString()
        val frameCountLayout = inflatedView.findViewById<LinearLayout>(R.id.frame_count_layout)
        frameCountLayout.setOnClickListener(FrameCountLayoutOnClickListener())


        //==========================================================================================
        //SHUTTER SPEED BUTTON
        shutterTextView = inflatedView.findViewById(R.id.shutter_text)
        updateShutterTextView()
        val shutterLayout = inflatedView.findViewById<LinearLayout>(R.id.shutter_layout)
        shutterLayout.setOnClickListener(ShutterLayoutOnClickListener())


        //==========================================================================================
        //APERTURE BUTTON
        apertureTextView = inflatedView.findViewById(R.id.aperture_text)
        updateApertureTextView()
        val apertureLayout = inflatedView.findViewById<LinearLayout>(R.id.aperture_layout)
        apertureLayout.setOnClickListener(ApertureLayoutOnClickListener())


        //==========================================================================================
        //FOCAL LENGTH BUTTON
        focalLengthTextView = inflatedView.findViewById(R.id.focal_length_text)
        updateFocalLengthTextView()
        val focalLengthLayout = inflatedView.findViewById<LinearLayout>(R.id.focal_length_layout)
        focalLengthLayout.setOnClickListener(FocalLengthLayoutOnClickListener())


        //==========================================================================================
        //EXPOSURE COMP BUTTON
        exposureCompTextView = inflatedView.findViewById(R.id.exposure_comp_text)
        exposureCompTextView.text = if (newFrame.exposureComp == null || newFrame.exposureComp == "0") "" else newFrame.exposureComp
        val exposureCompLayout = inflatedView.findViewById<LinearLayout>(R.id.exposure_comp_layout)
        exposureCompLayout.setOnClickListener(ExposureCompLayoutOnClickListener())


        //==========================================================================================
        //NO OF EXPOSURES BUTTON

        //Check that the number is bigger than zero.
        noOfExposuresTextView = inflatedView.findViewById(R.id.no_of_exposures_text)
        noOfExposuresTextView.text = newFrame.noOfExposures.toString()
        val noOfExposuresLayout = inflatedView.findViewById<LinearLayout>(R.id.no_of_exposures_layout)
        noOfExposuresLayout.setOnClickListener(NoOfExposuresLayoutOnClickListener())


        //==========================================================================================
        // LOCATION PICK DIALOG
        locationTextView = inflatedView.findViewById(R.id.location_text)
        updateLocationTextView()
        val locationProgressBar = inflatedView.findViewById<ProgressBar>(R.id.location_progress_bar)

        // If location is set but the formatted address is empty, try to find it
        newFrame.location?.let { location ->
            if (newFrame.formattedAddress == null || newFrame.formattedAddress?.isEmpty() == true) {
                // Make the ProgressBar visible to indicate that a query is being executed
                locationProgressBar.visibility = View.VISIBLE

                GeocodingAsyncTask { _, formattedAddress ->
                    locationProgressBar.visibility = View.INVISIBLE
                    newFrame.formattedAddress = if (formattedAddress.isNotEmpty()) formattedAddress else null
                    updateLocationTextView()
                }.execute(location.decimalLocation, resources.getString(R.string.google_maps_key))

            }
        }
        val clearLocation = inflatedView.findViewById<ImageView>(R.id.clear_location)
        clearLocation.setOnClickListener {
            newFrame.location = null
            newFrame.formattedAddress = null
            updateLocationTextView()
        }
        val locationLayout = inflatedView.findViewById<LinearLayout>(R.id.location_layout)
        locationLayout.setOnClickListener {
            val intent = Intent(activity, LocationPickActivity::class.java)
            intent.putExtra(ExtraKeys.LOCATION, newFrame.location)
            intent.putExtra(ExtraKeys.FORMATTED_ADDRESS, newFrame.formattedAddress)
            startActivityForResult(intent, PLACE_PICKER_REQUEST)
        }


        //==========================================================================================
        //FILTER BUTTON
        filtersTextView = inflatedView.findViewById(R.id.filter_text)
        updateFiltersTextView()

        // FILTER PICK DIALOG
        val filterLayout = inflatedView.findViewById<LinearLayout>(R.id.filter_layout)
        filterLayout.setOnClickListener(FilterLayoutOnClickListener())

        // FILTER ADD DIALOG
        val addFilterImageView = inflatedView.findViewById<ImageView>(R.id.add_filter)
        addFilterImageView.isClickable = true
        addFilterImageView.setOnClickListener {
            noteEditText.clearFocus()
            val dialog = EditFilterDialog()
            dialog.setTargetFragment(this@EditFrameDialog, ADD_FILTER)
            val arguments = Bundle()
            arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.NewFilter))
            arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.Add))
            dialog.arguments = arguments
            dialog.show(parentFragmentManager.beginTransaction(), EditFilterDialog.TAG)
        }


        //==========================================================================================
        //COMPLEMENTARY PICTURE
        pictureLayout = inflatedView.findViewById(R.id.picture_layout)
        pictureImageView = inflatedView.findViewById(R.id.iv_picture)
        pictureTextView = inflatedView.findViewById(R.id.picture_text)
        pictureLayout.setOnClickListener(PictureLayoutOnClickListener())


        //==========================================================================================
        //FLASH
        flashCheckBox = inflatedView.findViewById(R.id.flash_checkbox)
        flashCheckBox.isChecked = frame.flashUsed
        val flashUsedLayout = inflatedView.findViewById<View>(R.id.flash_layout)
        flashUsedLayout.setOnClickListener { flashCheckBox.isChecked = !flashCheckBox.isChecked }


        //==========================================================================================
        //LIGHT SOURCE
        lightSourceTextView = inflatedView.findViewById(R.id.light_source_text)
        val lightSource: String
        lightSource = try {
            resources.getStringArray(R.array.LightSource)[newFrame.lightSource]
        } catch (e: ArrayIndexOutOfBoundsException) {
            e.printStackTrace()
            ""
        }
        lightSourceTextView.text = if (newFrame.lightSource == 0) "" else lightSource
        val lightSourceLayout = inflatedView.findViewById<LinearLayout>(R.id.light_source_layout)
        lightSourceLayout.setOnClickListener(LightSourceLayoutOnClickListener())


        //==========================================================================================
        //FINALISE BUILDING THE DIALOG
        alert.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int ->
            val intent = Intent()
            targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_CANCELED, intent)
        }
        alert.setPositiveButton(positiveButton, null)
        val dialog = alert.create()

        //SOFT_INPUT_ADJUST_PAN: set to have a window pan when an input method is shown,
        // so it doesn't need to deal with resizing
        // but just panned by the framework to ensure the current input focus is visible
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        dialog.show()

        //User pressed OK, save.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(object : OnPositiveButtonClickListener(dialog) {
                    override fun onClick(view: View) {
                        super.onClick(view)
                        // Return the new entered name to the calling activity
                        val intent = Intent()
                        intent.putExtra(ExtraKeys.FRAME, frame)
                        targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, intent)
                    }
                })
        return dialog
    }

    /**
     * Calculates progress as integer ranging from 0 to 100
     * from current focal length, minimum focal length and maximum focal length.
     * Return 0 if current focal length equals minimum focal length, 100 if current
     * focal length equals maximum focal length, 50 if current focal length is midway
     * in the focal length range and so on...
     *
     * @param focalLength current focal length value
     * @param minValue minimum focal length value
     * @param maxValue maximum focal length value
     * @return integer from 0 to 100 describing progress
     */
    private fun calculateProgress(focalLength: Int, minValue: Int, maxValue: Int): Int {
        // progress = (newFocalLength - minValue) / (maxValue - minValue) * 100
        val result1 = focalLength - minValue.toDouble()
        val result2 = maxValue - minValue.toDouble()
        // No variables of type int can be used if parts of the calculation
        // result in fractions.
        val progressDouble = result1 / result2 * 100
        return progressDouble.roundToInt()
    }

    /**
     * Updates the filters TextView
     */
    private fun updateFiltersTextView() {
        filtersTextView.text = newFrame.filters.joinToString(separator = "\n") { "-${it.name}" }
    }

    /**
     * Method to finalize the member that is passed to the target fragment.
     * Also used to delete possibly unused older complementary pictures.
     */
    private fun onDialogDismiss() {
        frame.shutter = newFrame.shutter
        frame.aperture = newFrame.aperture
        frame.count = newFrame.count
        frame.note = noteEditText.text.toString()
        frame.date = dateTimeLayoutManager.dateTime
        frame.lens = newFrame.lens
        frame.location = newFrame.location
        frame.formattedAddress = newFrame.formattedAddress
        frame.exposureComp = newFrame.exposureComp
        frame.noOfExposures = newFrame.noOfExposures
        frame.focalLength = newFrame.focalLength
        frame.pictureFilename = newFrame.pictureFilename
        frame.filters = newFrame.filters
        frame.lightSource = newFrame.lightSource
        frame.flashUsed = flashCheckBox.isChecked
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == PLACE_PICKER_REQUEST && resultCode == Activity.RESULT_OK) {
            // Set the location
            if (data?.hasExtra(ExtraKeys.LOCATION) == true) {
                newFrame.location = data.getParcelableExtra(ExtraKeys.LOCATION)
            }
            // Set the formatted address
            if (data?.hasExtra(ExtraKeys.FORMATTED_ADDRESS) == true) {
                newFrame.formattedAddress = data.getStringExtra(ExtraKeys.FORMATTED_ADDRESS)
            }
            updateLocationTextView()
        }

        if (requestCode == ADD_LENS && resultCode == Activity.RESULT_OK) {
            // After Ok code.
            val lens: Lens = data?.getParcelableExtra(ExtraKeys.LENS) ?: return
            lens.id = database.addLens(lens)
            roll.camera?.let {
                database.addCameraLensLink(it, lens)
                mountableLenses?.add(lens)
            }
            lensTextView.text = lens.name
            apertureIncrements = lens.apertureIncrements
            checkApertureValueValidity()
            if (newFrame.focalLength > lens.maxFocalLength) newFrame.focalLength = lens.maxFocalLength
            else if (newFrame.focalLength < lens.minFocalLength) newFrame.focalLength = lens.minFocalLength
            newFrame.lens = lens
            updateFocalLengthTextView()
            resetFilters()
        }

        if (requestCode == ADD_FILTER && resultCode == Activity.RESULT_OK) {
            // After Ok code.
            val filter: Filter = data?.getParcelableExtra(ExtraKeys.FILTER) ?: return
            filter.id = database.addFilter(filter)
            newFrame.lens?.let { database.addLensFilterLink(filter, it) }
            newFrame.filters.add(filter)
            updateFiltersTextView()
        }

        if (requestCode == CAPTURE_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            // In case the picture decoding takes a long time, show the user, that the picture is being loaded.
            pictureTextView.setText(R.string.LoadingPicture)
            pictureTextView.visibility = View.VISIBLE
            // Decode and compress the picture on a background thread.
            Thread(Runnable {

                // The user has taken a new complementary picture. Update the possible new filename,
                // notify gallery app and set the complementary picture bitmap.
                val filename = tempPictureFilename ?: return@Runnable
                newFrame.pictureFilename = tempPictureFilename
                // Compress the picture file
                try {
                    ComplementaryPicturesManager.compressPictureFile(requireActivity(), filename)
                } catch (e: IOException) {
                    Toast.makeText(activity, R.string.ErrorCompressingComplementaryPicture, Toast.LENGTH_SHORT).show()
                }
                // Set the complementary picture ImageView on the UI thread.
                requireActivity().runOnUiThread { setComplementaryPicture() }
            }).start()
        }

        if (requestCode == SELECT_PICTURE_REQUEST && resultCode == Activity.RESULT_OK) {
            val selectedPictureUri = data?.data ?: return
            // In case the picture decoding takes a long time, show the user, that the picture is being loaded.
            pictureTextView.setText(R.string.LoadingPicture)
            pictureTextView.visibility = View.VISIBLE
            // Decode and compress the selected file on a background thread.
            Thread(Runnable {

                // Create the placeholder file in the complementary pictures directory.
                val pictureFile = ComplementaryPicturesManager.createNewPictureFile(requireActivity())
                try {
                    // Get the compressed bitmap from the Uri.
                    val pictureBitmap = ComplementaryPicturesManager
                            .getCompressedBitmap(requireActivity(), selectedPictureUri) ?: return@Runnable
                    try {
                        // Save the compressed bitmap to the placeholder file.
                        ComplementaryPicturesManager.saveBitmapToFile(pictureBitmap, pictureFile)
                        // Update the member reference and set the complementary picture.
                        newFrame.pictureFilename = pictureFile.name
                        // Set the complementary picture ImageView on the UI thread.
                        requireActivity().runOnUiThread { setComplementaryPicture() }
                    } catch (e: IOException) {
                        Toast.makeText(activity, R.string.ErrorSavingSelectedPicture, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: FileNotFoundException) {
                    Toast.makeText(activity, R.string.ErrorLocatingSelectedPicture, Toast.LENGTH_SHORT).show()
                }
            }).start()
        }

    }

    /**
     * Set the complementary picture ImageView with the newly selected/taken picture
     */
    private fun setComplementaryPicture() {

        // If the picture filename was not set, set text and return. Otherwise continue
        val filename = newFrame.pictureFilename
        if (filename == null) {
            pictureTextView.setText(R.string.ClickToAdd)
            return
        }
        val pictureFile = ComplementaryPicturesManager.getPictureFile(requireActivity(), filename)

        // If the picture file exists, set the picture ImageView.
        if (pictureFile.exists()) {

            // Set the visibilities first, so that the views in general are displayed
            // when the user scrolls down.
            pictureTextView.visibility = View.GONE
            pictureImageView.visibility = View.VISIBLE

            // Load the bitmap on a background thread
            Thread(Runnable {

                // Get the target ImageView height.
                // Because the complementary picture ImageView uses subclass SquareImageView,
                // the ImageView width should also be its height. Because the ImageView's
                // width is match_parent, we get the dialog's width instead.
                // If there is a problem getting the dialog window, use the resource dimension instead.
                val targetH = dialog?.window?.decorView?.width ?: resources.getDimension(R.dimen.ComplementaryPictureImageViewHeight).toInt()

                // Rotate the complementary picture ImageView if necessary
                var rotationTemp = 0
                try {
                    val exifInterface = ExifInterface(pictureFile.absolutePath)
                    val orientation = exifInterface
                            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> rotationTemp = 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> rotationTemp = 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> rotationTemp = 270
                    }
                } catch (ignore: IOException) {
                }
                val rotation = rotationTemp

                // Get the dimensions of the bitmap
                val options = BitmapFactory.Options()
                // Setting the inJustDecodeBounds property to true while decoding avoids memory allocation,
                // returning null for the bitmap object but setting outWidth, outHeight and outMimeType.
                options.inJustDecodeBounds = true
                BitmapFactory.decodeFile(pictureFile.absolutePath, options)
                val photoH = options.outHeight

                // Determine how much to scale down the image
                val scale = photoH / targetH

                // Set inJustDecodeBounds back to false, so that decoding returns a bitmap object.
                options.inJustDecodeBounds = false
                options.inSampleSize = scale

                // Decode the image file into a Bitmap sized to fill the view
                val bitmap = BitmapFactory.decodeFile(pictureFile.absolutePath, options)

                // Do UI changes on the UI thread.
                requireActivity().runOnUiThread {
                    pictureImageView.rotation = rotation.toFloat()
                    pictureImageView.setImageBitmap(bitmap)
                    val animation = AnimationUtils.loadAnimation(activity, R.anim.fade_in_fast)
                    pictureImageView.startAnimation(animation)
                }
            }).start()
        } else {
            pictureTextView.setText(R.string.PictureSetButNotFound)
        }
    }

    /**
     * Set the displayed aperture values depending on the lens's aperture increments
     * and its max and min aperture values. If no lens is selected, default to third stop
     * increments and don't limit the aperture values from either end.
     */
    private fun setDisplayedApertureValues() {
        //Get the array of displayed aperture values according to the set increments.
        displayedApertureValues = when (apertureIncrements) {
            0 -> requireActivity().resources.getStringArray(R.array.ApertureValuesThird)
            1 -> requireActivity().resources.getStringArray(R.array.ApertureValuesHalf)
            2 -> requireActivity().resources.getStringArray(R.array.ApertureValuesFull)
            else -> requireActivity().resources.getStringArray(R.array.ApertureValuesThird)
        }
        //Reverse the order if necessary.
        if (displayedApertureValues[0] == resources.getString(R.string.NoValue)) {
            // By reversing the order we can reverse the order in the NumberPicker too
            displayedApertureValues.reverse()
        }

        // Set the min and max values only if a lens is selected and they are set for the lens.
        // Otherwise the displayedApertureValues array will be left alone
        // (all aperture values available, since min and max were not defined).
        newFrame.lens?.let { lens ->
            val minIndex = displayedApertureValues.indexOfFirst { it == lens.minAperture }
            val maxIndex = displayedApertureValues.indexOfFirst { it == lens.maxAperture }
            if (minIndex != -1 && maxIndex != -1) {
                val apertureValues = displayedApertureValues.filterIndexed { index, _ ->
                    index in minIndex..maxIndex
                }.plus(resources.getString(R.string.NoValue))
                displayedApertureValues = apertureValues.toTypedArray()
            }
        }
    }

    /**
     * Called when the shutter speed value dialog is opened.
     * Set the values for the NumberPicker
     *
     * @param shutterPicker NumberPicker associated with the shutter speed value
     */
    private fun initialiseShutterPicker(shutterPicker: NumberPicker) {
        // Set the increments according to settings
        displayedShutterValues = when (shutterIncrements) {
            0 -> requireActivity().resources.getStringArray(R.array.ShutterValuesThird)
            1 -> requireActivity().resources.getStringArray(R.array.ShutterValuesHalf)
            2 -> requireActivity().resources.getStringArray(R.array.ShutterValuesFull)
            else -> requireActivity().resources.getStringArray(R.array.ShutterValuesThird)
        }
        //Reverse the order if necessary
        if (displayedShutterValues[0] == resources.getString(R.string.NoValue)) {
            // By reversing the order we can reverse the order in the NumberPicker too
            displayedShutterValues.reverse()
        }


        // Set the min and max values only if a lens is selected and they are set for the lens.
        // Otherwise the displayedShutterValues array will be left alone
        // (all shutter values available, since min and max were not defined).
        roll.camera?.let { camera ->
            val minIndex = displayedShutterValues.indexOfFirst { it == camera.minShutter }
            val maxIndex = displayedShutterValues.indexOfFirst { it == camera.maxShutter }
            displayedShutterValues = if (minIndex != -1 && maxIndex != -1) {
                val shutterValues = displayedShutterValues.filterIndexed { index, _ ->
                    index in minIndex..maxIndex
                }.plus("B").plus(resources.getString(R.string.NoValue))
                shutterValues.toTypedArray()
            } else {
                displayedShutterValues.toMutableList().also {
                    it.add(it.size - 1, "B")
                }.toTypedArray()
            }
        }

        shutterPicker.minValue = 0
        shutterPicker.maxValue = displayedShutterValues.size - 1
        shutterPicker.displayedValues = displayedShutterValues
        shutterPicker.value = displayedShutterValues.size - 1
        val initialValue = displayedShutterValues.indexOfFirst { it == newFrame.shutter }
        if (initialValue != -1) shutterPicker.value = initialValue
    }

    /**
     * Reset filters and update the filter button's text.
     */
    private fun resetFilters() {
        newFrame.filters.clear()
        filtersTextView.text = ""
    }

    /**
     * Updates the shutter speed value TextView's text.
     */
    private fun updateShutterTextView() {
        if (newFrame.shutter == null) {
            shutterTextView.text = ""
        } else {
            shutterTextView.text = newFrame.shutter
        }
    }

    /**
     * Updates the aperture value button's text.
     */
    private fun updateApertureTextView() {
        if (newFrame.aperture == null) {
            apertureTextView.text = ""
        } else {
            val newText = "f/${newFrame.aperture}"
            apertureTextView.text = newText
        }
    }

    /**
     * Updates the location button's text.
     */
    private fun updateLocationTextView() {
        when {
            newFrame.formattedAddress?.isNotEmpty() == true -> locationTextView.text = newFrame.formattedAddress
            newFrame.location != null -> {
                locationTextView.text = newFrame.location?.readableLocation
                        ?.replace("N ", "N\n")?.replace("S ", "S\n")
            }
            else -> {
                @SuppressLint("SetTextI18n")
                locationTextView.text = " \n "
            }
        }
    }

    /**
     * Updates the focal length TextView
     */
    private fun updateFocalLengthTextView() {
        focalLengthTextView.text = if (newFrame.focalLength == 0) "" else newFrame.focalLength.toString()
    }

    /**
     * When the currently selected lens is changed, check the validity of the currently
     * selected aperture value. I.e. it has to be within the new lens's aperture range.
     */
    private fun checkApertureValueValidity() {
        setDisplayedApertureValues()
        var apertureFound = false
        for (string in displayedApertureValues) {
            if (string == newFrame.aperture) {
                apertureFound = true
                break
            }
        }
        if (!apertureFound) {
            newFrame.aperture = null
            updateApertureTextView()
        }
    }

    /**
     * Class used by this class AlertDialog class and its subclasses. Implemented for positive button
     * onClick events.
     */
    internal open inner class OnPositiveButtonClickListener(private val dialog: AlertDialog) : View.OnClickListener {
        override fun onClick(view: View) {
            onDialogDismiss()
            dialog.dismiss()
        }

    }

    /**
     * Scroll change listener used to detect when the pictureLayout is visible.
     * Only then will the complementary picture be loaded.
     */
    private inner class OnScrollChangeListener internal constructor(context: Context, nestedScrollView: NestedScrollView,
                                                                    indicatorUp: View, indicatorDown: View) : ScrollIndicatorNestedScrollViewListener(context, nestedScrollView, indicatorUp, indicatorDown) {
        private var pictureLoaded = false
        override fun onScrollChange(v: NestedScrollView, scrollX: Int, scrollY: Int,
                                    oldScrollX: Int, oldScrollY: Int) {
            super.onScrollChange(v, scrollX, scrollY, oldScrollX, oldScrollY)
            val scrollBounds = Rect()
            v.getHitRect(scrollBounds)
            if (pictureLayout.getLocalVisibleRect(scrollBounds) && !pictureLoaded) {
                setComplementaryPicture()
                pictureLoaded = true
            }
        }
    }
    //==============================================================================================
    // LISTENER CLASSES USED TO OPEN NEW DIALOGS AFTER ONCLICK EVENTS
    /**
     * Listener class attached to shutter speed layout.
     * Opens a new dialog to display shutter speed options.
     */
    private inner class ShutterLayoutOnClickListener : View.OnClickListener {
        override fun onClick(view: View) {
            val builder = AlertDialog.Builder(activity)
            val inflater = requireActivity().layoutInflater
            @SuppressLint("InflateParams")
            val dialogView = inflater.inflate(R.layout.dialog_single_numberpicker, null)
            val shutterPicker = Utilities.fixNumberPicker(
                    dialogView.findViewById(R.id.number_picker)
            )
            initialiseShutterPicker(shutterPicker)
            shutterPicker.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            builder.setView(dialogView)
            builder.setTitle(resources.getString(R.string.ChooseShutterSpeed))
            builder.setPositiveButton(resources.getString(R.string.OK)) { _: DialogInterface?, _: Int ->
                newFrame.shutter =
                        if (shutterPicker.value != shutterPicker.maxValue) displayedShutterValues[shutterPicker.value]
                        else null
                updateShutterTextView()
            }
            builder.setNegativeButton(resources.getString(R.string.Cancel)) { _: DialogInterface?, _: Int -> }
            val dialog = builder.create()
            dialog.show()
        }
    }

    /**
     * Listener class attached to aperture value layout.
     * Opens a new dialog to display aperture value options.
     */
    private inner class ApertureLayoutOnClickListener : View.OnClickListener {
        /**
         * Variable to denote whether a custom aperture is being used or a predefined one.
         */
        private var manualOverride = false

        override fun onClick(view: View) {
            val builder = AlertDialog.Builder(activity)
            val inflater = requireActivity().layoutInflater
            @SuppressLint("InflateParams")
            val dialogView = inflater.inflate(R.layout.dialog_single_numberpicker_custom, null)
            val aperturePicker = dialogView.findViewById<NumberPicker>(R.id.number_picker)
            val editText = dialogView.findViewById<EditText>(R.id.edit_text)
            val customApertureSwitch: SwitchCompat = dialogView.findViewById(R.id.custom_aperture_switch)

            // Initialise the aperture value NumberPicker. The return value is true, if the
            // aperture value corresponds to a predefined value. The return value is false,
            // if the aperture value is a custom value.
            val apertureValueMatch = initialiseAperturePicker(aperturePicker)
            aperturePicker.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            builder.setView(dialogView)
            builder.setTitle(resources.getString(R.string.ChooseApertureValue))

            // If the aperture value did not match to any predefined aperture values, enable custom input.
            if (!apertureValueMatch) {
                customApertureSwitch.isChecked = true
                editText.setText(newFrame.aperture)
                aperturePicker.visibility = View.INVISIBLE
                editText.visibility = View.VISIBLE
                manualOverride = true
            } else {
                // Otherwise set manualOverride to false. This has to be done in case manual
                // override was enabled in a previous onClick event and the value of manualOverride
                // was left to true.
                manualOverride = false
            }

            // The user can switch between predefined aperture values and custom values using a switch.
            customApertureSwitch.setOnCheckedChangeListener { _: CompoundButton?, b: Boolean ->
                if (b) {
                    aperturePicker.visibility = View.INVISIBLE
                    editText.visibility = View.VISIBLE
                    manualOverride = true
                    val animation1 = AnimationUtils.loadAnimation(activity, R.anim.exit_to_right_alt)
                    val animation2 = AnimationUtils.loadAnimation(activity, R.anim.enter_from_left_alt)
                    aperturePicker.startAnimation(animation1)
                    editText.startAnimation(animation2)
                } else {
                    editText.visibility = View.INVISIBLE
                    aperturePicker.visibility = View.VISIBLE
                    manualOverride = false
                    val animation1 = AnimationUtils.loadAnimation(activity, R.anim.enter_from_right_alt)
                    val animation2 = AnimationUtils.loadAnimation(activity, R.anim.exit_to_left_alt)
                    aperturePicker.startAnimation(animation1)
                    editText.startAnimation(animation2)
                }
            }
            builder.setPositiveButton(resources.getString(R.string.OK)) { _: DialogInterface?, _: Int ->
                // Use the aperture value from the NumberPicker or EditText
                // depending on whether a custom value was used.
                newFrame.aperture = if (!manualOverride) {
                    if (aperturePicker.value != aperturePicker.maxValue) displayedApertureValues[aperturePicker.value] else null
                } else {
                    val customAperture = editText.text.toString()
                    if (customAperture.isNotEmpty()) customAperture else null
                }
                updateApertureTextView()
            }
            builder.setNegativeButton(resources.getString(R.string.Cancel)) { _: DialogInterface?, _: Int -> }
            val dialog = builder.create()
            dialog.show()
        }

        /**
         * Called when the aperture value dialog is opened.
         * Sets the values for the NumberPicker.
         *
         * @param aperturePicker NumberPicker associated with the aperture value
         * @return true if the current aperture value corresponds to some predefined aperture value,
         * false if not.
         */
        private fun initialiseAperturePicker(aperturePicker: NumberPicker): Boolean {
            setDisplayedApertureValues()
            //If no lens is selected, end here
            if (newFrame.lens == null) {
                aperturePicker.displayedValues = null
                aperturePicker.minValue = 0
                aperturePicker.maxValue = displayedApertureValues.size - 1
                aperturePicker.displayedValues = displayedApertureValues
                aperturePicker.value = displayedApertureValues.size - 1
                // Null aperture value is empty, which is a known value. Return true.
                if (newFrame.aperture == null) return true
                for (i in displayedApertureValues.indices) {
                    if (newFrame.aperture == displayedApertureValues[i]) {
                        aperturePicker.value = i
                        return true
                    }
                }
                return false
            }

            //Set the NumberPicker displayed values to null. If we set the displayed values
            //and the maxValue is smaller than the length of the new displayed values array,
            //ArrayIndexOutOfBounds is thrown.
            //Also if we set maxValue and the currently displayed values array length is smaller,
            //ArrayIndexOutOfBounds is thrown.
            //Setting displayed values to null solves this problem.
            aperturePicker.displayedValues = null
            aperturePicker.minValue = 0
            aperturePicker.maxValue = displayedApertureValues.size - 1
            aperturePicker.displayedValues = displayedApertureValues
            aperturePicker.value = displayedApertureValues.size - 1
            // Null aperture value is empty, which is a known value. Return true.
            if (newFrame.aperture == null) return true
            for (i in displayedApertureValues.indices) {
                if (newFrame.aperture == displayedApertureValues[i]) {
                    aperturePicker.value = i
                    return true
                }
            }
            return false
        }
    }

    /**
     * Listener class attached to frame count layout.
     * Opens a new dialog to display frame count options.
     */
    private inner class FrameCountLayoutOnClickListener : View.OnClickListener {
        override fun onClick(view: View) {
            val builder = AlertDialog.Builder(activity)
            val inflater = requireActivity().layoutInflater
            @SuppressLint("InflateParams") val dialogView = inflater.inflate(R.layout.dialog_single_numberpicker, null)
            val frameCountPicker = dialogView.findViewById<NumberPicker>(R.id.number_picker)
            frameCountPicker.minValue = 0
            frameCountPicker.maxValue = 100
            frameCountPicker.value = newFrame.count
            frameCountPicker.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            builder.setView(dialogView)
            builder.setTitle(resources.getString(R.string.ChooseFrameCount))
            builder.setPositiveButton(resources.getString(R.string.OK)) { _: DialogInterface?, _: Int ->
                newFrame.count = frameCountPicker.value
                frameCountTextView.text = newFrame.count.toString()
            }
            builder.setNegativeButton(resources.getString(R.string.Cancel)) { _: DialogInterface?, _: Int -> }
            val dialog = builder.create()
            dialog.show()
        }
    }

    /**
     * Listener class attached to lens layout.
     * Opens a new dialog to display lens options for current camera.
     * Check the validity of aperture value, focal length and filter after lens has been changed.
     */
    private inner class LensLayoutOnClickListener : View.OnClickListener {
        override fun onClick(view: View) {
            val listItems = listOf(resources.getString(R.string.NoLens))
                    .plus(mountableLenses?.map { it.name } ?: emptyList()).toTypedArray()
            val checkedItem = newFrame.lens?.let { lens ->
                mountableLenses?.indexOfFirst { it == lens }?.plus(1) ?: 0 // account for the 'No lens' option (+1)
            } ?: 0

            val builder = AlertDialog.Builder(activity)
            builder.setTitle(R.string.UsedLens)
            builder.setSingleChoiceItems(listItems, checkedItem) { dialog: DialogInterface, which: Int ->
                // Check if the lens was changed
                if (which > 0) {
                    // Get the new lens, also account for the 'No lens' option (which - 1).
                    val lens = mountableLenses?.get(which - 1) ?: return@setSingleChoiceItems
                    lensTextView.text = lens.name
                    newFrame.lens = lens
                    if (newFrame.focalLength > lens.maxFocalLength) {
                        newFrame.focalLength = lens.maxFocalLength
                    } else if (newFrame.focalLength < lens.minFocalLength) {
                        newFrame.focalLength = lens.minFocalLength
                    }
                    focalLengthTextView.text = if (newFrame.focalLength == 0) "" else newFrame.focalLength.toString()
                    apertureIncrements = lens.apertureIncrements

                    //Check the aperture value's validity against the new lens' properties.
                    checkApertureValueValidity()
                    // The lens was changed, reset filters
                    resetFilters()
                } else {
                    lensTextView.text = ""
                    newFrame.lens = null
                    newFrame.focalLength = 0
                    updateFocalLengthTextView()
                    apertureIncrements = 0
                    resetFilters()
                }
                dialog.dismiss()
            }
            builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
            val alert = builder.create()
            alert.show()
        }
    }

    /**
     * Listener class attached to filter layout.
     * Opens a new dialog to display filter options for current lens.
     */
    private inner class FilterLayoutOnClickListener : View.OnClickListener {
        override fun onClick(view: View) {
            // Get list of possible filters that can be selected.
            // If current lens is defined, use that to get linked filters.
            // Otherwise get all filters from database.
            val possibleFilters: List<Filter> =
                    newFrame.lens?.let { database.getLinkedFilters(it) }
                    ?: database.allFilters
            // Create a list with filter names to be shown on the multi choice dialog.
            val listItems = possibleFilters.map { it.name }.toTypedArray()
            // List where the mountable selections are stored.
            val filterSelections = possibleFilters.map {
                it to newFrame.filters.contains(it)
            }.toMutableList()
            // Bool array for preselected items in the multi choice list.
            val booleans = filterSelections.map { it.second }.toBooleanArray()

            val builder = AlertDialog.Builder(activity)
            builder.setTitle(R.string.UsedFilter)
            builder.setMultiChoiceItems(listItems, booleans) { _: DialogInterface?, which: Int, isChecked: Boolean ->
                filterSelections[which] = filterSelections[which].copy(second = isChecked)
            }
            builder.setPositiveButton(R.string.OK) { _: DialogInterface?, _: Int ->
                newFrame.filters = filterSelections.filter { it.second }.map { it.first }.toMutableList()
                updateFiltersTextView()
            }
            builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
            val alert = builder.create()
            alert.show()
        }
    }

    /**
     * Listener class attached to focal length layout.
     * Opens a new dialog to display focal length options.
     */
    private inner class FocalLengthLayoutOnClickListener : View.OnClickListener {
        override fun onClick(view: View) {
            val builder = AlertDialog.Builder(activity)
            val inflater = requireActivity().layoutInflater
            @SuppressLint("InflateParams")
            val dialogView = inflater.inflate(R.layout.dialog_seek_bar, null)
            val focalLengthSeekBar = dialogView.findViewById<SeekBar>(R.id.seek_bar)
            val focalLengthTextView = dialogView.findViewById<TextView>(R.id.value_text_view)

            // Get the min and max focal lengths
            val minValue: Int = newFrame.lens?.minFocalLength ?: 0
            val maxValue: Int = newFrame.lens?.maxFocalLength ?: 500

            // Set the SeekBar progress percent
            when {
                newFrame.focalLength > maxValue -> {
                    focalLengthSeekBar.progress = 100
                    focalLengthTextView.text = maxValue.toString()
                }
                newFrame.focalLength < minValue -> {
                    focalLengthSeekBar.progress = 0
                    focalLengthTextView.text = minValue.toString()
                }
                minValue == maxValue -> {
                    focalLengthSeekBar.progress = 50
                    focalLengthTextView.text = minValue.toString()
                }
                else -> {
                    focalLengthSeekBar.progress = calculateProgress(newFrame.focalLength, minValue, maxValue)
                    focalLengthTextView.text = newFrame.focalLength.toString()
                }
            }

            // When the user scrolls the SeekBar, change the TextView to indicate
            // the current focal length converted from the progress (int i)
            focalLengthSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                    val focalLength = minValue + (maxValue - minValue) * i / 100
                    focalLengthTextView.text = focalLength.toString()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    // Do nothing
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    // Do nothing
                }
            })
            val increaseFocalLength = dialogView.findViewById<TextView>(R.id.increase_focal_length)
            val decreaseFocalLength = dialogView.findViewById<TextView>(R.id.decrease_focal_length)
            increaseFocalLength.setOnClickListener {
                var focalLength = focalLengthTextView.text.toString().toInt()
                if (focalLength < maxValue) {
                    ++focalLength
                    focalLengthSeekBar.progress = calculateProgress(focalLength, minValue, maxValue)
                    focalLengthTextView.text = focalLength.toString()
                }
            }
            decreaseFocalLength.setOnClickListener {
                var focalLength = focalLengthTextView.text.toString().toInt()
                if (focalLength > minValue) {
                    --focalLength
                    focalLengthSeekBar.progress = calculateProgress(focalLength, minValue, maxValue)
                    focalLengthTextView.text = focalLength.toString()
                }
            }
            builder.setView(dialogView)
            builder.setTitle(resources.getString(R.string.ChooseFocalLength))
            builder.setPositiveButton(resources.getString(R.string.OK)) { _: DialogInterface?, _: Int ->
                newFrame.focalLength = focalLengthTextView.text.toString().toInt()
                updateFocalLengthTextView()
            }
            builder.setNegativeButton(resources.getString(R.string.Cancel)) { _: DialogInterface?, _: Int -> }
            val dialog = builder.create()
            dialog.show()
        }
    }

    /**
     * Listener class attached to exposure compensation layout.
     * Opens a new dialog to display exposure compensation options.
     */
    private inner class ExposureCompLayoutOnClickListener : View.OnClickListener {
        override fun onClick(view: View) {
            val builder = AlertDialog.Builder(activity)
            val inflater = requireActivity().layoutInflater
            @SuppressLint("InflateParams") val dialogView = inflater.inflate(R.layout.dialog_single_numberpicker, null)
            val exposureCompPicker = Utilities.fixNumberPicker(
                    dialogView.findViewById(R.id.number_picker)
            )
            displayedExposureCompValues = when (exposureCompIncrements) {
                0 -> requireActivity().resources.getStringArray(R.array.CompValues)
                1 -> requireActivity().resources.getStringArray(R.array.CompValuesHalf)
                else -> requireActivity().resources.getStringArray(R.array.CompValues)
            }
            exposureCompPicker.minValue = 0
            exposureCompPicker.maxValue = displayedExposureCompValues.size - 1
            exposureCompPicker.displayedValues = displayedExposureCompValues
            exposureCompPicker.value = floor(displayedExposureCompValues.size / 2.toDouble()).toInt()
            val initialValue = displayedExposureCompValues.indexOfFirst { it == newFrame.exposureComp }
            if (initialValue != -1) exposureCompPicker.value = initialValue
            exposureCompPicker.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            builder.setView(dialogView)
            builder.setTitle(resources.getString(R.string.ChooseExposureComp))
            builder.setPositiveButton(resources.getString(R.string.OK)) { _: DialogInterface?, _: Int ->
                newFrame.exposureComp = displayedExposureCompValues[exposureCompPicker.value]
                exposureCompTextView.text =
                        if (newFrame.exposureComp == null || newFrame.exposureComp == "0") ""
                        else newFrame.exposureComp
            }
            builder.setNegativeButton(resources.getString(R.string.Cancel)) { _: DialogInterface?, _: Int -> }
            val dialog = builder.create()
            dialog.show()
        }
    }

    /**
     * Listener class attached to number of exposures layout.
     * Opens a new dialog to display number of exposures options.
     */
    private inner class NoOfExposuresLayoutOnClickListener : View.OnClickListener {
        override fun onClick(view: View) {
            val builder = AlertDialog.Builder(activity)
            val inflater = requireActivity().layoutInflater
            @SuppressLint("InflateParams")
            val dialogView = inflater.inflate(R.layout.dialog_single_numberpicker, null)
            val noOfExposuresPicker = dialogView.findViewById<NumberPicker>(R.id.number_picker)
            noOfExposuresPicker.minValue = 1
            noOfExposuresPicker.maxValue = 10
            noOfExposuresPicker.value = 1
            if (newFrame.noOfExposures > 1) {
                noOfExposuresPicker.value = newFrame.noOfExposures
            }
            noOfExposuresPicker.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            builder.setView(dialogView)
            builder.setTitle(resources.getString(R.string.ChooseNoOfExposures))
            builder.setPositiveButton(resources.getString(R.string.OK)) { _: DialogInterface?, _: Int ->
                newFrame.noOfExposures = noOfExposuresPicker.value
                noOfExposuresTextView.text = newFrame.noOfExposures.toString()
            }
            builder.setNegativeButton(resources.getString(R.string.Cancel)
            ) { _: DialogInterface?, _: Int -> }
            val dialog = builder.create()
            dialog.show()
        }
    }

    /**
     * Listener class attached to complementary picture layout.
     * Shows various actions regarding the complementary picture.
     */
    private inner class PictureLayoutOnClickListener : View.OnClickListener {
        override fun onClick(view: View) {
            val pictureActionDialogBuilder = AlertDialog.Builder(activity)

            // If a complementary picture was not set, set only the two first options
            val items: Array<String> = if (newFrame.pictureFilename == null) {
                arrayOf(
                        getString(R.string.TakeNewComplementaryPicture),
                        getString(R.string.SelectPictureFromGallery)
                )
            } else {
                arrayOf(
                        getString(R.string.TakeNewComplementaryPicture),
                        getString(R.string.SelectPictureFromGallery),
                        getString(R.string.AddPictureToGallery),
                        getString(R.string.RotateRight90Degrees),
                        getString(R.string.RotateLeft90Degrees),
                        getString(R.string.Clear)
                )
            }

            // Add the items and the listener
            pictureActionDialogBuilder.setItems(items) { dialogInterface: DialogInterface, i: Int ->
                when (i) {
                    0 -> {
                        dialogInterface.dismiss()
                        startPictureActivity()
                    }
                    1 -> {
                        val selectPictureIntent = Intent(Intent.ACTION_PICK)
                        selectPictureIntent.type = "image/*"
                        startActivityForResult(selectPictureIntent, SELECT_PICTURE_REQUEST)
                    }
                    2 -> {
                        try {
                            ComplementaryPicturesManager.addPictureToGallery(requireActivity(), newFrame.pictureFilename)
                            Toast.makeText(activity, R.string.PictureAddedToGallery, Toast.LENGTH_SHORT).show()
                        } catch (e: IOException) {
                            Toast.makeText(activity, R.string.ErrorAddingPictureToGallery, Toast.LENGTH_LONG).show()
                        }
                        dialogInterface.dismiss()
                    }
                    3 -> rotateComplementaryPictureRight()
                    4 -> rotateComplementaryPictureLeft()
                    5 -> {
                        newFrame.pictureFilename = null
                        pictureImageView.visibility = View.GONE
                        pictureTextView.visibility = View.VISIBLE
                        pictureTextView.setText(R.string.ClickToAdd)
                        dialogInterface.dismiss()
                    }
                }
            }
            pictureActionDialogBuilder.setNegativeButton(R.string.Cancel) { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
            }
            pictureActionDialogBuilder.create().show()
        }

        /**
         * Starts a camera activity to take a new complementary picture
         */
        private fun startPictureActivity() {
            // Check if the camera feature is available
            if (!requireActivity().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                Toast.makeText(activity, R.string.NoCameraFeatureWasFound, Toast.LENGTH_SHORT).show()
                return
            }
            // Advance with taking the picture
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(requireActivity().packageManager) != null) {
                // Create the file where the photo should go
                val pictureFile = ComplementaryPicturesManager.createNewPictureFile(requireActivity())
                tempPictureFilename = pictureFile.name
                val photoURI: Uri
                //Android Nougat requires that the file is given via FileProvider
                photoURI = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(requireContext(), requireContext().applicationContext
                            .packageName + ".provider", pictureFile)
                } else {
                    Uri.fromFile(pictureFile)
                }
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, CAPTURE_IMAGE_REQUEST)
            }
        }

        /**
         * Rotate the complementary picture ImageView 90 degrees clockwise and
         * set the complementary picture orientation with ComplementaryPicturesManager.
         */
        private fun rotateComplementaryPictureRight() {
            val filename = newFrame.pictureFilename ?: return
            try {
                ComplementaryPicturesManager.rotatePictureRight(requireActivity(), filename)
                pictureImageView.rotation = pictureImageView.rotation + 90
                val animation = AnimationUtils.loadAnimation(activity, R.anim.rotate_right)
                pictureImageView.startAnimation(animation)
            } catch (e: IOException) {
                Toast.makeText(activity, R.string.ErrorWhileEditingPicturesExifData, Toast.LENGTH_SHORT).show()
            }
        }

        /**
         * Rotate the complementary picture ImageView 90 degrees counterclockwise and set
         * the complementary picture orientation with ComplementaryPicturesManager.
         */
        private fun rotateComplementaryPictureLeft() {
            val filename = newFrame.pictureFilename ?: return
            try {
                ComplementaryPicturesManager.rotatePictureLeft(requireActivity(), filename)
                pictureImageView.rotation = pictureImageView.rotation - 90
                val animation = AnimationUtils.loadAnimation(activity, R.anim.rotate_left)
                pictureImageView.startAnimation(animation)
            } catch (e: IOException) {
                Toast.makeText(activity, R.string.ErrorWhileEditingPicturesExifData, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Listener class attached to light source layout.
     * Shows different light source options as a simple list.
     */
    private inner class LightSourceLayoutOnClickListener : View.OnClickListener {
        override fun onClick(v: View) {
            val builder = AlertDialog.Builder(activity)
            val checkedItem = newFrame.lightSource
            builder.setSingleChoiceItems(R.array.LightSource, checkedItem) { dialog: DialogInterface, which: Int ->
                newFrame.lightSource = which
                lightSourceTextView.text =
                        if (newFrame.lightSource == 0) ""
                        else resources.getStringArray(R.array.LightSource)[which]
                dialog.dismiss()
            }
            builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
            builder.create().show()
        }
    }

}