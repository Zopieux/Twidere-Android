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

package org.mariotaku.twidere.adapter

import android.content.Context
import android.database.Cursor
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.mariotaku.twidere.R
import org.mariotaku.twidere.adapter.iface.IDirectMessagesAdapter
import org.mariotaku.twidere.constant.*
import org.mariotaku.twidere.model.ParcelableDirectMessage
import org.mariotaku.twidere.model.ParcelableDirectMessageCursorIndices
import org.mariotaku.twidere.model.ParcelableMedia
import org.mariotaku.twidere.model.UserKey
import org.mariotaku.twidere.util.*
import org.mariotaku.twidere.view.CardMediaContainer
import org.mariotaku.twidere.view.ShapedImageView
import org.mariotaku.twidere.view.holder.IncomingMessageViewHolder
import org.mariotaku.twidere.view.holder.MessageViewHolder
import java.lang.ref.WeakReference

class MessageConversationAdapter(context: Context) : BaseRecyclerViewAdapter<ViewHolder>(context), IDirectMessagesAdapter {
    private val mOutgoingMessageColor: Int
    private val mIncomingMessageColor: Int
    override val profileImageEnabled: Boolean

    @ShapedImageView.ShapeStyle
    override val profileImageStyle: Int
    override val mediaPreviewStyle: Int

    private val mInflater: LayoutInflater
    val mediaLoadingHandler: MediaLoadingHandler
    override val textSize: Float

    private var mCursor: Cursor? = null
    private var mIndices: ParcelableDirectMessageCursorIndices? = null
    val linkify: TwidereLinkify
    val onMediaClickListener: CardMediaContainer.OnMediaClickListener

    init {
        mInflater = LayoutInflater.from(context)
        linkify = TwidereLinkify(DirectMessageOnLinkClickHandler(context, null, preferences))
        textSize = preferences[textSizeKey].toFloat()
        profileImageEnabled = preferences[displayProfileImageKey]
        profileImageStyle = Utils.getProfileImageStyle(preferences[profileImageStyleKey])
        mediaPreviewStyle = Utils.getMediaPreviewStyle(preferences[mediaPreviewStyleKey])
        mediaLoadingHandler = MediaLoadingHandler(R.id.media_preview_progress)
        mIncomingMessageColor = ThemeUtils.getUserAccentColor(context)
        mOutgoingMessageColor = ThemeUtils.getCardBackgroundColor(context,
                ThemeUtils.getThemeBackgroundOption(context), ThemeUtils.getUserThemeBackgroundAlpha(context))
        onMediaClickListener = EventListener(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        when (viewType) {
            ITEM_VIEW_TYPE_MESSAGE_INCOMING -> {
                val view = mInflater.inflate(R.layout.card_item_message_conversation_incoming, parent, false)
                val holder = IncomingMessageViewHolder(this, view)
                holder.setMessageColor(mIncomingMessageColor)
                holder.setTextSize(this.textSize)
                return holder
            }
            ITEM_VIEW_TYPE_MESSAGE_OUTGOING -> {
                val view = mInflater.inflate(R.layout.card_item_message_conversation_outgoing, parent, false)
                val holder = MessageViewHolder(this, view)
                holder.setMessageColor(mOutgoingMessageColor)
                holder.setTextSize(this.textSize)
                return holder
            }
        }
        throw UnsupportedOperationException("Unknown viewType " + viewType)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder.itemViewType) {
            ITEM_VIEW_TYPE_MESSAGE_INCOMING, ITEM_VIEW_TYPE_MESSAGE_OUTGOING -> {
                val c = mCursor
                c!!.moveToPosition(getCursorPosition(position))
                (holder as MessageViewHolder).displayMessage(c, mIndices)
            }
        }
    }

    private fun getCursorPosition(position: Int): Int {
        return position
    }

    override fun getItemViewType(position: Int): Int {
        val c = mCursor
        c!!.moveToPosition(position)
        if (c.getInt(mIndices!!.is_outgoing) == 1) {
            return ITEM_VIEW_TYPE_MESSAGE_OUTGOING
        } else {
            return ITEM_VIEW_TYPE_MESSAGE_INCOMING
        }
    }

    override fun getItemCount(): Int {
        val c = mCursor ?: return 0
        return c.count
    }

    override fun findItem(id: Long): ParcelableDirectMessage? {
        var i = 0
        val count = itemCount
        while (i < count) {
            if (getItemId(i) == id) return getDirectMessage(i)
            i++
        }
        return null
    }

    fun getDirectMessage(position: Int): ParcelableDirectMessage? {
        val c = mCursor
        if (c == null || c.isClosed) return null
        c.moveToPosition(position)
        val accountKey = UserKey.valueOf(c.getString(mIndices!!.account_key))
        val messageId = c.getLong(mIndices!!.id)
        return Utils.findDirectMessageInDatabases(context, accountKey, messageId)
    }

    fun setCursor(cursor: Cursor?) {
        if (cursor != null) {
            mIndices = ParcelableDirectMessageCursorIndices(cursor)
        } else {
            mIndices = null
        }
        mCursor = cursor
        notifyDataSetChanged()
    }

    internal class EventListener(adapter: MessageConversationAdapter) : CardMediaContainer.OnMediaClickListener {

        private val adapterRef: WeakReference<MessageConversationAdapter>

        init {
            this.adapterRef = WeakReference(adapter)
        }

        override fun onMediaClick(view: View, media: ParcelableMedia, accountKey: UserKey, extraId: Long) {
            val adapter = adapterRef.get()
            val message = adapter.getDirectMessage(extraId.toInt()) ?: return
            IntentUtils.openMedia(adapter.context, message, media, null,
                    adapter.preferences[newDocumentApiKey])
        }

    }

    companion object {

        private val ITEM_VIEW_TYPE_MESSAGE_OUTGOING = 1
        private val ITEM_VIEW_TYPE_MESSAGE_INCOMING = 2
    }
}
