package com.port.camtraffic.ui.sync

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.port.camtraffic.R
import dagger.android.support.AndroidSupportInjection
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class SyncFragment : Fragment() {

    @Inject lateinit var factory: ViewModelProvider.Factory
    private val viewModel: SyncViewModel by viewModels { factory }
    private val compositeDisposable = CompositeDisposable()

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.sync_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.syncState.observe(this, Observer {
            if (it) {
                findNavController().popBackStack()
            } else {
                val syncDisposable = viewModel.synchronize()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread()).subscribe { syncState, error ->
                        if (error != null){
                            Timber.e(error)
                            return@subscribe
                        }
                        viewModel.setSyncState(syncState)
                    }
                compositeDisposable.add(syncDisposable)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if ( !compositeDisposable.isDisposed) compositeDisposable.dispose()
    }
}
