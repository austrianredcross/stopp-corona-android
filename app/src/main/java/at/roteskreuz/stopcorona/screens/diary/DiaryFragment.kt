package at.roteskreuz.stopcorona.screens.diary

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.constants.Constants
import at.roteskreuz.stopcorona.screens.base.CoronaPortraitBaseActivity
import at.roteskreuz.stopcorona.screens.diary.day.startDiaryDayFragment
import at.roteskreuz.stopcorona.skeleton.core.screens.base.activity.startFragmentActivity
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import at.roteskreuz.stopcorona.skeleton.core.utils.observeOnMainThread
import at.roteskreuz.stopcorona.utils.datesTo
import at.roteskreuz.stopcorona.utils.string
import com.itextpdf.text.Document
import com.itextpdf.text.Font
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfWriter
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.fragment_diary.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.threeten.bp.LocalDate
import timber.log.Timber
import java.io.OutputStream


class DiaryFragment : BaseFragment(R.layout.fragment_diary) {

    companion object {

        private const val REQUEST_CODE_CREATE_PDF_FILE =
            Constants.Request.REQUEST_DIARY_FRAGMENT + 1
    }


    override val isToolbarVisible: Boolean = true
    override val isToolbarMenuVisible: Boolean = true

    private val viewModel: DiaryViewModel by viewModel()

    private val controller: DiaryController by lazy {
        DiaryController(
            requireContext(),
            onDiaryEntryClick = { day ->
                startDiaryDayFragment(day)
            },
            onAdditionalInformationClick = {
                startDiaryExplanationFragment()
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(contentRecyclerView) {
            setController(controller)
        }


        disposables += viewModel.observeDbDiaryEntry()
            .observeOnMainThread()
            .subscribe { dbDiaryEntry ->
                LocalDate.now().minusDays(14).datesTo(LocalDate.now()).sortedByDescending { it }
                    .forEach { date ->
                        controller.dates[date] = mutableListOf()
                    }
                dbDiaryEntry.forEach { entry ->
                    if (controller.dates.containsKey(entry.date)) {
                        controller.dates[entry.date]?.add(entry)
                    } else {
                        viewModel.deleteDiaryEntry(entry.id)
                    }
                }
                controller.requestModelBuild()
            }
    }

    override fun onInitActionBar(actionBar: ActionBar?, toolbar: Toolbar?) {
        super.onInitActionBar(actionBar, toolbar)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar?.setNavigationIcon(R.drawable.ic_back)
        toolbar?.setNavigationContentDescription(R.string.general_back)
    }

    override fun getTitle(): String? {
        return getString(R.string.diary_overview_title)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.diaryPdfExport -> {
                createPdf()
                true
            }
            R.id.diaryShare -> {
                viewModel.getText(requireContext(), controller.dates) { text ->
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, text)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    startActivity(shareIntent)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun createPdf() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, requireContext().string(R.string.diary_export_filename) + ".pdf")
        }
        startActivityForResult(intent, REQUEST_CODE_CREATE_PDF_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_CREATE_PDF_FILE) {
            intent?.data?.let { intentData ->
                viewModel.getText(requireContext(), controller.dates) { text ->
                    val fileOutputStream: OutputStream? =
                        activity?.contentResolver?.openOutputStream(intentData)
                    val doc = Document()
                    try {
                        PdfWriter.getInstance(doc, fileOutputStream)
                        doc.open()
                        val p1 = Paragraph(text)
                        val paraFont = Font(Font.FontFamily.COURIER)
                        p1.alignment = Paragraph.ALIGN_LEFT
                        p1.font = paraFont
                        doc.add(p1)
                    } catch (e: Exception) {
                        Timber.d(e)
                    } finally {
                        doc.close()
                        fileOutputStream?.flush()
                        fileOutputStream?.close()
                    }
                }
            }
        }
    }
}


fun Fragment.startDiaryFragment() {
    startFragmentActivity<CoronaPortraitBaseActivity>(
        fragmentName = DiaryFragment::class.java.name
    )
}