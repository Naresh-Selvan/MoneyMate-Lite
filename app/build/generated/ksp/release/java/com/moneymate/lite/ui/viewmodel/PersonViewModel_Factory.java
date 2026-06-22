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
public final class PersonViewModel_Factory implements Factory<PersonViewModel> {
  private final Provider<PersonRepository> repositoryProvider;

  private final Provider<LoanFileRepository> fileRepositoryProvider;

  public PersonViewModel_Factory(Provider<PersonRepository> repositoryProvider,
      Provider<LoanFileRepository> fileRepositoryProvider) {
    this.repositoryProvider = repositoryProvider;
    this.fileRepositoryProvider = fileRepositoryProvider;
  }

  @Override
  public PersonViewModel get() {
    return newInstance(repositoryProvider.get(), fileRepositoryProvider.get());
  }

  public static PersonViewModel_Factory create(Provider<PersonRepository> repositoryProvider,
      Provider<LoanFileRepository> fileRepositoryProvider) {
    return new PersonViewModel_Factory(repositoryProvider, fileRepositoryProvider);
  }

  public static PersonViewModel newInstance(PersonRepository repository,
      LoanFileRepository fileRepository) {
    return new PersonViewModel(repository, fileRepository);
  }
}
