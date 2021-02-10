package at.roteskreuz.stopcorona.screens.dashboard.report_healthy

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.RelativeLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.R
import kotlinx.android.synthetic.main.fragment_report_healthy.*
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Dialog to end quarantine.
 */
class ReportHealthyFragment : DialogFragment() {

    private val viewModel: ReportHealthyViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_report_healthy, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnEndQuarantine.setOnClickListener {
            viewModel.revokeMedicalConfirmation()
            dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // for a full width dialog
        val root = RelativeLayout(activity)
        root.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        context?.let {context ->
            val dialog = Dialog(context)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(root)
            dialog.window?.let { window ->
                window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            return dialog
        }

        return dialog!!
    }
}

fun Fragment.showReportHealthyFragment() {
    ReportHealthyFragment().show(requireFragmentManager(), ReportHealthyFragment::class.java.name)
}