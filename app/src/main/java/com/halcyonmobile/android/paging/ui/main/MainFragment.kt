package com.halcyonmobile.android.paging.ui.main

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.map
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.halcyonmobile.android.paging.PagedState
import com.halcyonmobile.android.paging.R
import com.halcyonmobile.android.paging.databinding.MainFragmentBinding
import org.koin.android.viewmodel.ext.android.viewModel

class MainFragment : Fragment(R.layout.main_fragment) {

    private val viewModel: MainViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = MainFragmentBinding.bind(view)

        val adapter = GitHubRepoAdapter(viewModel.toAdapterListener())
        binding.recycler.adapter = adapter
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.errorMessage.setOnClickListener { viewModel.onRefresh() }
        binding.refreshLayout.setOnRefreshListener { viewModel.onRefresh() }
        viewModel.state.observe(viewLifecycleOwner, Observer {
            binding.recycler.post {
                when (it) {
                    is PagedState.LoadingMore -> adapter.onLoadingMore()
                    is PagedState.ErrorLoadingMore -> adapter.onErrorLoadingMore()
                    is PagedState.EndReached -> adapter.onEndReached()
                    else -> Unit
                }
            }
        })
        viewModel.dataStream.observe(viewLifecycleOwner, Observer {
            adapter.submitList(it.orEmpty())
        })
        viewModel.state.map { it is PagedState.LoadingInitial }.observe(viewLifecycleOwner, Observer {
            binding.initialProgressBar.isVisible = it
        })
        viewModel.state.map { it is PagedState.Refreshing }.observe(viewLifecycleOwner, Observer {
            binding.refreshLayout.isRefreshing = it
        })
        viewModel.state.map { it !is PagedState.LoadingInitial && it !is PagedState.ErrorLoadingInitial }.observe(viewLifecycleOwner, Observer {
            binding.recycler.isVisible = it
        })
        viewModel.state.map { it is PagedState.ErrorLoadingInitial }.observe(viewLifecycleOwner, Observer {
            binding.errorMessage.isVisible = it
        })
        viewModel.state.map { it }.observe(viewLifecycleOwner, Observer {
            if (it is PagedState.RefreshingError) {
                Snackbar.make(binding.root, "Refreshing error", Snackbar.LENGTH_LONG).show()
                it.cause.printStackTrace()
            }
        })
    }

    companion object {
        fun MainViewModel.toAdapterListener(): GitHubRepoAdapter.GitHubRepoAdapterListener = object : GitHubRepoAdapter.GitHubRepoAdapterListener {
            override fun onDataAtPositionBound(position: Int) = this@toAdapterListener.onDataAtPositionBound(position)

            override fun onRetryLoadingMoreClicked() = onRetryLoadingMore()
        }
    }
}
