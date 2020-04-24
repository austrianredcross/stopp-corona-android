package at.roteskreuz.stopcorona.skeleton.core.utils.views

import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import com.jakewharton.rxbinding3.InitialValueObservable
import io.reactivex.Observer
import io.reactivex.android.MainThreadDisposable

/**
 * Editable text watcher as Rx stream.
 */
class EditableTextViewTextObservable(
    private val view: TextView
) : InitialValueObservable<CharSequence>() {

    override val initialValue: CharSequence
        get() = view.text

    override fun subscribeListener(observer: Observer<in CharSequence>) {
        val listener = Listener(view, observer)
        observer.onSubscribe(listener)
        view.addTextChangedListener(listener)
    }

    internal class Listener(private val view: TextView, private val observer: Observer<in CharSequence>) :
        MainThreadDisposable(), TextWatcher {

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            // do nothing
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            // do nothing
        }

        override fun afterTextChanged(s: Editable) {
            if (!isDisposed) {
                view.removeTextChangedListener(this)
                observer.onNext(s)
                view.addTextChangedListener(this)
            }
        }

        override fun onDispose() {
            view.removeTextChangedListener(this)
        }
    }
}