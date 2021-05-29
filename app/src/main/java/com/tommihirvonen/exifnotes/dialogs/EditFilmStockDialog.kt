package com.tommihirvonen.exifnotes.dialogs

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.DialogFilmBinding
import com.tommihirvonen.exifnotes.datastructures.FilmStock
import com.tommihirvonen.exifnotes.utilities.ExtraKeys
import com.tommihirvonen.exifnotes.utilities.Utilities

class EditFilmStockDialog : DialogFragment() {

    private lateinit var binding: DialogFilmBinding

    @SuppressLint("SetTextI18n", "InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val layoutInflater = requireActivity().layoutInflater
        binding = DialogFilmBinding.inflate(layoutInflater)

        val title = requireArguments().getString(ExtraKeys.TITLE)
        val positiveButtonText = requireArguments().getString(ExtraKeys.POSITIVE_BUTTON)
        val filmStock = requireArguments().getParcelable(ExtraKeys.FILM_STOCK) ?: FilmStock()

        val builder = AlertDialog.Builder(requireActivity())
        builder.setView(binding.root)
                .setCustomTitle(Utilities.buildCustomDialogTitleTextView(requireActivity(), title))

        binding.manufacturerEditText.setText(filmStock.make)
        binding.filmStockEditText.setText(filmStock.model)
        binding.isoEditText.setText(filmStock.iso.toString())
        binding.isoEditText.filters = arrayOf<InputFilter>(IsoInputFilter())

        try {
            binding.spinnerFilmType.setSelection(filmStock.type)
        } catch (ignore: ArrayIndexOutOfBoundsException) {}

        try {
            binding.spinnerFilmProcess.setSelection(filmStock.process)
        } catch (ignore: ArrayIndexOutOfBoundsException) {}

        builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int ->
            setFragmentResult("EditFilmStockDialog", Bundle())
        }.setPositiveButton(positiveButtonText, null)

        val dialog = builder.create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val manufacturerName = binding.manufacturerEditText.text.toString()
            val filmStockName = binding.filmStockEditText.text.toString()
            if (manufacturerName.isEmpty() || filmStockName.isEmpty()) {
                Toast.makeText(requireActivity(), R.string.ManufacturerOrFilmStockNameCannotBeEmpty,
                        Toast.LENGTH_SHORT).show()
            } else {
                filmStock.make = manufacturerName
                filmStock.model = filmStockName
                try {
                    filmStock.iso = binding.isoEditText.text.toString().toInt()
                } catch (ignored: NumberFormatException) {
                    filmStock.iso = 0
                }
                filmStock.type = binding.spinnerFilmType.selectedItemPosition
                filmStock.process = binding.spinnerFilmProcess.selectedItemPosition

                dialog.dismiss()
                val bundle = Bundle()
                bundle.putParcelable(ExtraKeys.FILM_STOCK, filmStock)
                setFragmentResult("EditFilmStockDialog", bundle)
            }
        }

        return dialog
    }

    /**
     * Private InputFilter class used to make sure user entered ISO values are between 0 and 1000000
     */
    private inner class IsoInputFilter : InputFilter {
        override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int,
                            dend: Int): CharSequence? {
            try {
                val input = (dest.toString() + source.toString()).toInt()
                if (input in 0..1000000) return null
            } catch (ignored: NumberFormatException) { }
            return ""
        }
    }
}