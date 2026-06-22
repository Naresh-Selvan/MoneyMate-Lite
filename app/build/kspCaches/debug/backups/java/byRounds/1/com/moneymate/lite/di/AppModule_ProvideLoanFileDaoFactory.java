package com.moneymate.lite.di;

import com.moneymate.lite.data.AppDatabase;
import com.moneymate.lite.data.dao.LoanFileDao;
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
public final class AppModule_ProvideLoanFileDaoFactory implements Factory<LoanFileDao> {
  private final Provider<AppDatabase> databaseProvider;

  public AppModule_ProvideLoanFileDaoFactory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public LoanFileDao get() {
    return provideLoanFileDao(databaseProvider.get());
  }

  public static AppModule_ProvideLoanFileDaoFactory create(Provider<AppDatabase> databaseProvider) {
    return new AppModule_ProvideLoanFileDaoFactory(databaseProvider);
  }

  public static LoanFileDao provideLoanFileDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideLoanFileDao(database));
  }
}
