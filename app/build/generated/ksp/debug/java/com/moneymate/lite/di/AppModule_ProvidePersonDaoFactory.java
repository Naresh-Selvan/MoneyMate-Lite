package com.moneymate.lite.di;

import com.moneymate.lite.data.AppDatabase;
import com.moneymate.lite.data.dao.PersonDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class AppModule_ProvidePersonDaoFactory implements Factory<PersonDao> {
  private final Provider<AppDatabase> databaseProvider;

  public AppModule_ProvidePersonDaoFactory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public PersonDao get() {
    return providePersonDao(databaseProvider.get());
  }

  public static AppModule_ProvidePersonDaoFactory create(Provider<AppDatabase> databaseProvider) {
    return new AppModule_ProvidePersonDaoFactory(databaseProvider);
  }

  public static PersonDao providePersonDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.providePersonDao(database));
  }
}
