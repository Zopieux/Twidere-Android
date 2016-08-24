package org.mariotaku.twidere.adapter

import android.content.Context
import android.support.v4.text.BidiFormatter
import android.support.v7.widget.RecyclerView
import org.mariotaku.kpreferences.KPreferences
import org.mariotaku.twidere.R
import org.mariotaku.twidere.adapter.iface.*
import org.mariotaku.twidere.adapter.iface.ILoadMoreSupportAdapter.IndicatorPosition
import org.mariotaku.twidere.constant.*
import org.mariotaku.twidere.model.ParcelableStatus
import org.mariotaku.twidere.model.ParcelableUser
import org.mariotaku.twidere.model.ParcelableUserList
import org.mariotaku.twidere.model.UserKey
import org.mariotaku.twidere.util.*
import org.mariotaku.twidere.util.dagger.GeneralComponentHelper
import org.mariotaku.twidere.view.holder.iface.IStatusViewHolder
import javax.inject.Inject

/**
 * Created by mariotaku on 16/1/22.
 */
class DummyItemAdapter @JvmOverloads constructor(
        override val context: Context,
        override val twidereLinkify: TwidereLinkify = TwidereLinkify(null),
        private val adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>? = null
) : IStatusesAdapter<Any>, IUsersAdapter<Any>, IUserListsAdapter<Any>, SharedPreferenceConstants {

    override val mediaLoadingHandler: MediaLoadingHandler
    @Inject
    lateinit var preferences: KPreferences
    @Inject
    override lateinit var mediaLoader: MediaLoaderWrapper
    @Inject
    override lateinit var twitterWrapper: AsyncTwitterWrapper
    @Inject
    override lateinit var userColorNameManager: UserColorNameManager
    @Inject
    override lateinit var bidiFormatter: BidiFormatter

    override var profileImageStyle: Int = 0
    override var mediaPreviewStyle: Int = 0
    override var textSize: Float = 0f
    override var linkHighlightingStyle: Int = 0
    override var nameFirst: Boolean = false
    override var profileImageEnabled: Boolean = false
    override var sensitiveContentEnabled: Boolean = false
    override var mediaPreviewEnabled: Boolean = false
    override var isShowAbsoluteTime: Boolean = false
    override var followClickListener: IUsersAdapter.FriendshipClickListener? = null
    override var requestClickListener: IUsersAdapter.RequestClickListener? = null
    override var statusClickListener: IStatusViewHolder.StatusClickListener? = null
    override var userClickListener: IUsersAdapter.UserClickListener? = null
    override var showAccountsColor: Boolean = false
    override var useStarsForLikes: Boolean = false

    private var showCardActions: Boolean = false
    private var showingActionCardPosition = RecyclerView.NO_POSITION

    init {
        GeneralComponentHelper.build(context).inject(this)
        mediaLoadingHandler = MediaLoadingHandler(R.id.media_preview_progress)
        updateOptions()
    }

    fun setShouldShowAccountsColor(shouldShowAccountsColor: Boolean) {
        this.showAccountsColor = shouldShowAccountsColor
    }


    override fun getItemCount(): Int {
        return 0
    }

    override var loadMoreIndicatorPosition: Long
        @IndicatorPosition
        get() = ILoadMoreSupportAdapter.NONE
        set(@IndicatorPosition position) {

        }

    override var loadMoreSupportedPosition: Long
        @IndicatorPosition
        get() = ILoadMoreSupportAdapter.NONE
        set(@IndicatorPosition supported) {

        }

    override fun getStatus(position: Int): ParcelableStatus? {
        if (adapter is ParcelableStatusesAdapter) {
            return adapter.getStatus(position)
        } else if (adapter is VariousItemsAdapter) {
            return adapter.getItem(position) as ParcelableStatus
        }
        return null
    }

    override val statusCount: Int
        get() = 0

    override val rawStatusCount: Int
        get() = 0

    override fun getStatusId(position: Int): String? {
        return null
    }

    override fun getStatusTimestamp(position: Int): Long {
        return -1
    }

    override fun getStatusPositionKey(position: Int): Long {
        return -1
    }

    override fun getAccountKey(position: Int): UserKey? {
        return null
    }

    override fun findStatusById(accountKey: UserKey, statusId: String): ParcelableStatus? {
        return null
    }

    override fun isCardActionsShown(position: Int): Boolean {
        if (position == RecyclerView.NO_POSITION) return showCardActions
        return showCardActions || showingActionCardPosition == position
    }

    override fun showCardActions(position: Int) {
        if (showingActionCardPosition != RecyclerView.NO_POSITION && adapter != null) {
            adapter.notifyItemChanged(showingActionCardPosition)
        }
        showingActionCardPosition = position
        if (position != RecyclerView.NO_POSITION && adapter != null) {
            adapter.notifyItemChanged(position)
        }
    }

    override fun getUser(position: Int): ParcelableUser? {
        if (adapter is ParcelableUsersAdapter) {
            return adapter.getUser(position)
        } else if (adapter is VariousItemsAdapter) {
            return adapter.getItem(position) as ParcelableUser
        }
        return null
    }

    override val userCount: Int
        get() = 0

    override val userListsCount: Int
        get() = 0

    override val gapClickListener: IGapSupportedAdapter.GapClickListener?
        get() = null
    override val userListClickListener: IUserListsAdapter.UserListClickListener?
        get() = null

    override fun getUserId(position: Int): String? {
        return null
    }

    override fun getUserList(position: Int): ParcelableUserList? {
        return null
    }

    override fun getUserListId(position: Int): String? {
        return null
    }

    override fun setData(data: Any?): Boolean {
        return false
    }

    override fun isGapItem(position: Int): Boolean {
        return false
    }

    fun updateOptions() {
        profileImageStyle = Utils.getProfileImageStyle(preferences[profileImageStyleKey])
        mediaPreviewStyle = Utils.getMediaPreviewStyle(preferences[mediaPreviewStyleKey])
        textSize = preferences[textSizeKey].toFloat()
        nameFirst = preferences[nameFirstKey]
        profileImageEnabled = preferences[displayProfileImageKey]
        mediaPreviewEnabled = preferences[mediaPreviewKey]
        sensitiveContentEnabled = preferences[displaySensitiveContentsKey]
        showCardActions = !preferences[hideCardActionsKey]
        linkHighlightingStyle = Utils.getLinkHighlightingStyleInt(preferences[linkHighlightOptionKey])
        useStarsForLikes = preferences[iWantMyStarsBackKey]
        isShowAbsoluteTime = preferences[showAbsoluteTimeKey]
    }
}
