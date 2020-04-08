package com.halcyonmobile.android.paging.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.halcyonmobile.android.core.model.GitHubRepo
import com.halcyonmobile.android.paging.R
import com.halcyonmobile.android.paging.databinding.ItemGithubRepoBinding
import com.halcyonmobile.android.paging.databinding.ItemLoadingMoreBinding
import com.halcyonmobile.android.paging.databinding.ItemLoadingMoreErrorBinding

class GitHubRepoAdapter(private val listener: GitHubRepoAdapterListener) : ListAdapter<GitHubRepo, RecyclerView.ViewHolder>(GitHubRepoDiffUtilItemCallback()) {

    private var loading: Boolean = true
    private var errorLoading: Boolean = false

    fun onLoadingMore() {
        if (loading && !errorLoading) return
        val previousLoading = loading
        val previousErrorLoading = errorLoading

        loading = true
        errorLoading = false
        if (previousLoading || previousErrorLoading) {
            notifyItemChanged(super.getItemCount())
        } else {
            notifyItemInserted(super.getItemCount())
        }
    }

    fun onEndReached() {
        if (!loading && !errorLoading) return

        loading = false
        errorLoading = false
        notifyItemRemoved(super.getItemCount())
    }

    fun onErrorLoadingMore() {
        if (!loading && errorLoading) return
        val previousLoading = loading
        val previousErrorLoading = errorLoading

        loading = false
        errorLoading = true
        if (previousLoading || previousErrorLoading) {
            notifyItemChanged(super.getItemCount())
        } else {
            notifyItemInserted(super.getItemCount())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            1 -> GitHubRepoViewHolder(parent)
            2 -> LoadingMoreViewHolder(parent)
            else -> LoadingMoreErrorViewHolder(parent, listener)
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is GitHubRepoViewHolder) {
            holder.bind(getItem(position))
            listener.onDataAtPositionBound(position)
        }
    }

    override fun getItemCount(): Int = super.getItemCount() + if (loading || errorLoading) 1 else 0

    override fun getItemViewType(position: Int): Int =
        when {
            position < super.getItemCount() -> 1
            loading -> 2
            else -> 3
        }

    interface GitHubRepoAdapterListener : LoadingMoreErrorViewHolder.RetryLoadingMoreClickListener {

        fun onDataAtPositionBound(position: Int)
    }

    class LoadingMoreErrorViewHolder(
        viewGroup: ViewGroup,
        retryLoadingMoreClickListener: RetryLoadingMoreClickListener
    ) : BindingViewHolder<ItemLoadingMoreErrorBinding>(viewGroup, R.layout.item_loading_more_error) {
        init {
            binding.root.setOnClickListener { retryLoadingMoreClickListener.onRetryLoadingMoreClicked() }
        }

        interface RetryLoadingMoreClickListener {
            fun onRetryLoadingMoreClicked()
        }
    }

    class LoadingMoreViewHolder(viewGroup: ViewGroup) : BindingViewHolder<ItemLoadingMoreBinding>(viewGroup, R.layout.item_loading_more)

    class GitHubRepoViewHolder(viewGroup: ViewGroup) : BindingViewHolder<ItemGithubRepoBinding>(viewGroup, R.layout.item_github_repo) {

        fun bind(gitHubRepo: GitHubRepo) {
            binding.name.text = gitHubRepo.name
            binding.numberOfWatchers.text = gitHubRepo.numberOfWatchers.toString()
            binding.url.text = gitHubRepo.htmlUrl
            binding.executePendingBindings()
        }
    }

    open class BindingViewHolder<Binding : ViewDataBinding> private constructor(val binding: Binding) : RecyclerView.ViewHolder(binding.root) {

        constructor(viewGroup: ViewGroup, @LayoutRes layoutRes: Int) : this(DataBindingUtil.inflate(LayoutInflater.from(viewGroup.context), layoutRes, viewGroup, false))
    }

    class GitHubRepoDiffUtilItemCallback : DiffUtil.ItemCallback<GitHubRepo>() {
        override fun areItemsTheSame(oldItem: GitHubRepo, newItem: GitHubRepo): Boolean =
            oldItem.nodeId == newItem.nodeId

        override fun areContentsTheSame(oldItem: GitHubRepo, newItem: GitHubRepo): Boolean =
            oldItem == newItem

    }
}