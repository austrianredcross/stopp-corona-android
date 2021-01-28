package at.roteskreuz.stopcorona.screens.mandatory_update

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.utils.startPlatformAppStore
import kotlinx.android.synthetic.main.fragment_mandatory_update.*

class MandatoryUpdateFragment : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        isCancelable = false
        return inflater.inflate(R.layout.fragment_mandatory_update, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txtTitle.contentDescription = getString(R.string.mandatory_update_title) + getString(R.string.accessibility_heading_2)

        btnGoToPlatformStore.setOnClickListener {
            val appPackageName = requireContext().packageName
            startPlatformAppStore(appPackageName)
        }
    }

    // use Apptheme for full screen
    override fun getTheme(): Int {
        return R.style.AppTheme
    }
}

fun Fragment.showMandatoryUpdateFragment() {
    MandatoryUpdateFragment().show(requireFragmentManager(), MandatoryUpdateFragment::class.java.name)
}