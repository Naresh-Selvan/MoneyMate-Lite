package com.moneymate.lite.data.repository;

import com.moneymate.lite.data.dao.LoanDao;
import com.moneymate.lite.data.dao.PaymentDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class LoanRepository_Factory implements Factory<LoanRepository> {
  private final Provider<LoanDao> loanDaoProvider;

  private final Provider<PaymentDao> paymentDaoProvider;

  public LoanRepository_Factory(Provider<LoanDao> loanDaoProvider,
      Provider<PaymentDao> paymentDaoProvider) {
    this.loanDaoProvider = loanDaoProvider;
    this.paymentDaoProvider = paymentDaoProvider;
  }

  @Override
  public LoanRepository get() {
    return newInstance(loanDaoProvider.get(), paymentDaoProvider.get());
  }

  public static LoanRepository_Factory create(Provider<LoanDao> loanDaoProvider,
      Provider<PaymentDao> paymentDaoProvider) {
    return new LoanRepository_Factory(loanDaoProvider, paymentDaoProvider);
  }

  public static LoanRepository newInstance(LoanDao loanDao, PaymentDao paymentDao) {
    return new LoanRepository(loanDao, paymentDao);
  }
}
