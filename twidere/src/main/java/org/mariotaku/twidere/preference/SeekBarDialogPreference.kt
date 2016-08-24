/*
 * 				Twidere - Twitter client for Android
 * 
 *  Copyright (C) 2012-2014 Mariotaku Lee <mariotaku.lee@gmail.com>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mariotaku.twidere.preference

import android.content.Context
import android.content.res.TypedArray
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.support.v7.preference.DialogPreference
import android.support.v7.preference.PreferenceFragmentCompat
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.widget.SeekBar
import android.widget.TextView

import org.mariotaku.twidere.R
import org.mariotaku.twidere.fragment.ThemedPreferenceDialogFragmentCompat
import org.mariotaku.twidere.preference.iface.IDialogPreference

/**
 * A [DialogPreference] that provides a user with the means to select an
 * integer from a [SeekBar], and persist it.

 * @author lukehorvat
 */
class SeekBarDialogPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet = null, defStyle: Int = R.attr.dialogPreferenceStyle) : DialogPreference(context, attrs, defStyle), IDialogPreference {

    var minProgress: Int = 0
        set(minProgress) {
            field = minProgress
            progress = Math.max(progress, this.minProgress)
        }
    var maxProgress: Int = 0
        set(maxProgress) {
            field = maxProgress
            progress = Math.min(progress, this.maxProgress)
        }
    /**
     * @param progress Real progress multiplied by steps
     */
    var progress: Int = 0
        set(progress) {
            var progress = progress
            progress = Math.max(Math.min(progress, maxProgress), minProgress)

            if (progress != this.progress) {
                field = progress
                persistInt(progress)
                callChangeListener(progress)
                notifyChanged()
            }
        }
    private var step: Int = 0
        set

    var progressTextSuffix: CharSequence? = null

    init {

        // get attributes specified in XML
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.SeekBarDialogPreference, 0, 0)
        try {
            minProgress = a.getInteger(R.styleable.SeekBarDialogPreference_min, DEFAULT_MIN_PROGRESS)
            maxProgress = a.getInteger(R.styleable.SeekBarDialogPreference_max, DEFAULT_MAX_PROGRESS)
            step = a.getInteger(R.styleable.SeekBarDialogPreference_step, DEFAULT_STEP)
            progressTextSuffix = a.getString(R.styleable.SeekBarDialogPreference_progressTextSuffix)
        } finally {
            a.recycle()
        }

        // set layout
        dialogLayoutResource = R.layout.dialog_preference_seek_bar
        setPositiveButtonText(android.R.string.ok)
        setNegativeButtonText(android.R.string.cancel)
        dialogIcon = null
    }

    override fun onGetDefaultValue(a: TypedArray?, index: Int): Any {
        return a!!.getInt(index, DEFAULT_PROGRESS)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        // check whether we saved the state in onSaveInstanceState()
        if (state == null || state.javaClass != SavedState::class.java) {
            // didn't save the state, so call superclass
            super.onRestoreInstanceState(state)
            return
        }

        // restore the state
        val myState = state as SavedState?
        minProgress = myState!!.minProgress
        maxProgress = myState.maxProgress
        progress = myState.progress
        step = myState.step

        super.onRestoreInstanceState(myState.getSuperState())
    }

    override fun onSaveInstanceState(): Parcelable {
        // save the instance state so that it will survive screen orientation
        // changes and other events that may temporarily destroy it
        val superState = super.onSaveInstanceState()

        // set the state's value with the class member that holds current
        // setting value
        val myState = SavedState(superState)
        myState.minProgress = minProgress
        myState.maxProgress = maxProgress
        myState.progress = progress
        myState.step = step
        return myState
    }

    override fun onSetInitialValue(restore: Boolean, defaultValue: Any?) {
        progress = if (restore) getPersistedInt(DEFAULT_PROGRESS) else defaultValue as Int?
    }

    override fun displayDialog(fragment: PreferenceFragmentCompat) {
        val df = SeekBarDialogPreferenceFragment.newInstance(key)
        df.setTargetFragment(fragment, 0)
        df.show(fragment.fragmentManager, key)
    }

    class SeekBarDialogPreferenceFragment : ThemedPreferenceDialogFragmentCompat() {

        private var mProgressText: TextView? = null
        private var mSeekBar: SeekBar? = null

        override fun onBindDialogView(view: View) {
            super.onBindDialogView(view)
            val preference = preference as SeekBarDialogPreference
            val message = preference.dialogMessage
            val dialogMessageText = view.findViewById(R.id.text_dialog_message) as TextView
            dialogMessageText.text = message
            dialogMessageText.visibility = if (TextUtils.isEmpty(message)) View.GONE else View.VISIBLE

            mProgressText = view.findViewById(R.id.text_progress) as TextView

            mSeekBar = view.findViewById(R.id.seek_bar) as SeekBar
            mSeekBar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    // update text that displays the current SeekBar progress value
                    // note: this does not persist the progress value. that is only
                    // ever done in setProgress()
                    val step = preference.step
                    val minProgress = preference.minProgress
                    val progressStr = (progress * step + minProgress).toString()
                    val progressTextSuffix = preference.progressTextSuffix
                    if (progressTextSuffix == null) {
                        mProgressText!!.text = progressStr
                    } else {
                        mProgressText!!.text = progressStr + progressTextSuffix.toString()
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            })
            val maxProgress = preference.maxProgress
            val minProgress = preference.minProgress
            val step = preference.step
            val progress = preference.progress
            mSeekBar!!.max = Math.ceil((maxProgress - minProgress) / step.toDouble()).toInt()
            mSeekBar!!.progress = Math.ceil((progress - minProgress) / step.toDouble()).toInt()
        }

        override fun onDialogClosed(positive: Boolean) {
            if (positive) {
                val preference = preference as SeekBarDialogPreference
                val minProgress = preference.minProgress
                val step = preference.step
                val realProgress = mSeekBar!!.progress * step + minProgress
                if (preference.callChangeListener(realProgress)) {
                    preference.progress = realProgress
                }
            }
        }

        companion object {

            fun newInstance(key: String): SeekBarDialogPreferenceFragment {
                val fragment = SeekBarDialogPreferenceFragment()
                val args = Bundle()
                args.putString(PreferenceDialogFragmentCompat.ARG_KEY, key)
                fragment.arguments = args
                return fragment
            }
        }
    }

    private class SavedState : Preference.BaseSavedState {
        internal var minProgress: Int = 0
        internal var maxProgress: Int = 0
        internal var progress: Int = 0
        internal var step: Int = 0

        constructor(source: Parcel) : super(source) {

            minProgress = source.readInt()
            maxProgress = source.readInt()
            progress = source.readInt()
            step = source.readInt()
        }

        constructor(superState: Parcelable) : super(superState) {
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)

            dest.writeInt(minProgress)
            dest.writeInt(maxProgress)
            dest.writeInt(progress)
            dest.writeInt(step)
        }

        companion object {

            @SuppressWarnings("unused")
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(`in`: Parcel): SavedState {
                    return SavedState(`in`)
                }

                override fun newArray(size: Int): Array<SavedState> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    companion object {
        private val DEFAULT_MIN_PROGRESS = 0
        private val DEFAULT_MAX_PROGRESS = 100
        private val DEFAULT_PROGRESS = 0
        private val DEFAULT_STEP = 1
    }
}
