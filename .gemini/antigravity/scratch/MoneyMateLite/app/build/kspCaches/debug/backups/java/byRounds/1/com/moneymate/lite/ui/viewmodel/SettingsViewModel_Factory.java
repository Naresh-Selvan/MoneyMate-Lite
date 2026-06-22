package com.moneymate.lite.ui.viewmodel;

import com.moneymate.lite.data.firebase.RestoreHelper;
import com.moneymate.lite.data.firebase.UploadHelper;
import com.moneymate.lite.data.repository.LoanFileRepository;
import com.moneymate.lite.data.repository.LoanRepository;
import com.moneymate.lite.data.repository.PaymentRepository;
import com.moneymate.lite.data.repository.PersonRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<UploadHelper> uploadHelperProvider;

  private final Provider<RestoreHelper> restoreHelperProvider;

  private final Provider<LoanFileRepository> loanFileRepositoryProvider;

  private final Provider<PersonRepository> personRepositoryProvider;

  private final Provider<LoanRepository> loanRepositoryProvider;

  private final Provider<PaymentRepository> paymentRepositoryProvider;

  public SettingsViewModel_Factory(Provider<UploadHelper> uploadHelperProvider,
      Provider<RestoreHelper> restoreHelperProvider,
      Provider<LoanFileRepository> loanFileRepositoryProvider,
      Provider<PersonRepository> personRepositoryProvider,
      Provider<LoanRepository> loanRepositoryProvider,
      Provider<PaymentRepository> paymentRepositoryProvider) {
    this.uploadHelperProvider = uploadHelperProvider;
    this.restoreHelperProvider = restoreHelperProvider;
    this.loanFileRepositoryProvider = loanFileRepositoryProvider;
    this.personRepositoryProvider = personRepositoryProvider;
    this.loanRepositoryProvider = loanRepositoryProvider;
    this.paymentRepositoryProvider = paymentRepositoryProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(uploadHelperProvider.get(), restoreHelperProvider.get(), loanFileRepositoryProvider.get(), personRepositoryProvider.get(), loanRepositoryProvider.get(), paymentRepositoryProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<UploadHelper> uploadHelperProvider,
      Provider<RestoreHelper> restoreHelperProvider,
      Provider<LoanFileRepository> loanFileRepositoryProvider,
      Provider<PersonRepository> personRepositoryProvider,
      Provider<LoanRepository> loanRepositoryProvider,
      Provider<PaymentRepository> paymentRepositoryProvider) {
    return new SettingsViewModel_Factory(uploadHelperProvider, restoreHelperProvider, loanFileRepositoryProvider, personRepositoryProvider, loanRepositoryProvider, paymentRepositoryProvider);
  }

  public static SettingsViewModel newInstance(UploadHelper uploadHelper,
      RestoreHelper restoreHelper, LoanFileRepository loanFileRepository,
      PersonRepository personRepository, LoanRepository loanRepository,
      PaymentRepository paymentRepository) {
    return new SettingsViewModel(uploadHelper, restoreHelper, loanFileRepository, personRepository, loanRepository, paymentRepository);
  }
}
