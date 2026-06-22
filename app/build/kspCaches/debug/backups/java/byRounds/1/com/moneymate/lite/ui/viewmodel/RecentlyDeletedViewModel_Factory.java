package com.moneymate.lite.ui.viewmodel;

import com.moneymate.lite.data.repository.LoanFileRepository;
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
public final class RecentlyDeletedViewModel_Factory implements Factory<RecentlyDeletedViewModel> {
  private final Provider<LoanFileRepository> fileRepositoryProvider;

  private final Provider<PersonRepository> personRepositoryProvider;

  public RecentlyDeletedViewModel_Factory(Provider<LoanFileRepository> fileRepositoryProvider,
      Provider<PersonRepository> personRepositoryProvider) {
    this.fileRepositoryProvider = fileRepositoryProvider;
    this.personRepositoryProvider = personRepositoryProvider;
  }

  @Override
  public RecentlyDeletedViewModel get() {
    return newInstance(fileRepositoryProvider.get(), personRepositoryProvider.get());
  }

  public static RecentlyDeletedViewModel_Factory create(
      Provider<LoanFileRepository> fileRepositoryProvider,
      Provider<PersonRepository> personRepositoryProvider) {
    return new RecentlyDeletedViewModel_Factory(fileRepositoryProvider, personRepositoryProvider);
  }

  public static RecentlyDeletedViewModel newInstance(LoanFileRepository fileRepository,
      PersonRepository personRepository) {
    return new RecentlyDeletedViewModel(fileRepository, personRepository);
  }
}
