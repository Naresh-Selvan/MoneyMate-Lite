package com.moneymate.lite.ui.viewmodel;

import com.moneymate.lite.data.repository.LoanRepository;
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
public final class PaymentViewModel_Factory implements Factory<PaymentViewModel> {
  private final Provider<LoanRepository> loanRepositoryProvider;

  public PaymentViewModel_Factory(Provider<LoanRepository> loanRepositoryProvider) {
    this.loanRepositoryProvider = loanRepositoryProvider;
  }

  @Override
  public PaymentViewModel get() {
    return newInstance(loanRepositoryProvider.get());
  }

  public static PaymentViewModel_Factory create(Provider<LoanRepository> loanRepositoryProvider) {
    return new PaymentViewModel_Factory(loanRepositoryProvider);
  }

  public static PaymentViewModel newInstance(LoanRepository loanRepository) {
    return new PaymentViewModel(loanRepository);
  }
}
