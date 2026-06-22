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
public final class LoanViewModel_Factory implements Factory<LoanViewModel> {
  private final Provider<LoanRepository> repositoryProvider;

  public LoanViewModel_Factory(Provider<LoanRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public LoanViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static LoanViewModel_Factory create(Provider<LoanRepository> repositoryProvider) {
    return new LoanViewModel_Factory(repositoryProvider);
  }

  public static LoanViewModel newInstance(LoanRepository repository) {
    return new LoanViewModel(repository);
  }
}
