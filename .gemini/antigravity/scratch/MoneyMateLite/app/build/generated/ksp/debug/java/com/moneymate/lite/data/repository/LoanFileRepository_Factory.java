package com.moneymate.lite.data.repository;

import com.moneymate.lite.data.dao.LoanFileDao;
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
public final class LoanFileRepository_Factory implements Factory<LoanFileRepository> {
  private final Provider<LoanFileDao> loanFileDaoProvider;

  public LoanFileRepository_Factory(Provider<LoanFileDao> loanFileDaoProvider) {
    this.loanFileDaoProvider = loanFileDaoProvider;
  }

  @Override
  public LoanFileRepository get() {
    return newInstance(loanFileDaoProvider.get());
  }

  public static LoanFileRepository_Factory create(Provider<LoanFileDao> loanFileDaoProvider) {
    return new LoanFileRepository_Factory(loanFileDaoProvider);
  }

  public static LoanFileRepository newInstance(LoanFileDao loanFileDao) {
    return new LoanFileRepository(loanFileDao);
  }
}
