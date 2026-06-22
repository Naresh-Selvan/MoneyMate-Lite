package com.moneymate.lite.di;

import com.moneymate.lite.data.AppDatabase;
import com.moneymate.lite.data.dao.PaymentDao;
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
public final class AppModule_ProvidePaymentDaoFactory implements Factory<PaymentDao> {
  private final Provider<AppDatabase> databaseProvider;

  public AppModule_ProvidePaymentDaoFactory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public PaymentDao get() {
    return providePaymentDao(databaseProvider.get());
  }

  public static AppModule_ProvidePaymentDaoFactory create(Provider<AppDatabase> databaseProvider) {
    return new AppModule_ProvidePaymentDaoFactory(databaseProvider);
  }

  public static PaymentDao providePaymentDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.providePaymentDao(database));
  }
}
