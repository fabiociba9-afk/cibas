package com.aistudio.executivogo.trnsp.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aistudio.executivogo.trnsp.data.ExecutivoGoRepository

class MainViewModelFactory(
    private val application: Application,
    private val repository: ExecutivoGoRepository = ExecutivoGoRepository()
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Classe ViewModel desconhecida")
    }
}
