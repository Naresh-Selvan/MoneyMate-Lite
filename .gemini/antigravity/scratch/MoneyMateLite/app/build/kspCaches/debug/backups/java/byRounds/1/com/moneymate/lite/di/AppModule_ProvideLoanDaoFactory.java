package com.moneymate.lite.di;

import com.moneymate.lite.data.AppDatabase;
import com.moneymate.lite.data.dao.LoanDao;
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
public final class AppModule_ProvideLoanDaoFactory implements Factory<LoanDao> {
  private final Provider<AppDatabase> databaseProvider;

  public AppModule_ProvideLoanDaoFactory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public LoanDao get() {
    return provideLoanDao(databaseProvider.get());
  }

  public static AppModule_ProvideLoanDaoFactory create(Provider<AppDatabase> databaseProvider) {
    return new AppModule_ProvideLoanDaoFactory(databaseProvider);
  }

  public static LoanDao provideLoanDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideLoanDao(database));
  }
}
