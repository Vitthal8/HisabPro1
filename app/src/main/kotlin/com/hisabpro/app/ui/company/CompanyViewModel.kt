package com.hisabpro.app.ui.company

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hisabpro.app.data.model.BusinessProfile
import com.hisabpro.app.data.repository.BusinessManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DeleteCompanyUiState {
    object Idle : DeleteCompanyUiState()
    object Deleting : DeleteCompanyUiState()
    data class Success(val deletedBusinessId: String) : DeleteCompanyUiState()
    data class Error(val message: String) : DeleteCompanyUiState()
}

/**
 * ViewModel managing company/business profile lifecycle, switching, and cascading deletion.
 */
class CompanyViewModel(application: Application) : AndroidViewModel(application) {

    private val businessManager = BusinessManager.getInstance(application)

    private val _deleteState = MutableStateFlow<DeleteCompanyUiState>(DeleteCompanyUiState.Idle)
    val deleteState: StateFlow<DeleteCompanyUiState> = _deleteState.asStateFlow()

    val businesses: StateFlow<List<BusinessProfile>> = businessManager.businesses
    val activeBusiness: StateFlow<BusinessProfile> = businessManager.activeBusiness

    fun deleteCompany(businessId: String) {
        viewModelScope.launch {
            _deleteState.value = DeleteCompanyUiState.Deleting
            val result = businessManager.deleteBusinessCascade(businessId)
            if (result.isSuccess) {
                _deleteState.value = DeleteCompanyUiState.Success(businessId)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to delete company profile."
                _deleteState.value = DeleteCompanyUiState.Error(errorMsg)
            }
        }
    }

    fun resetDeleteState() {
        _deleteState.value = DeleteCompanyUiState.Idle
    }
}
