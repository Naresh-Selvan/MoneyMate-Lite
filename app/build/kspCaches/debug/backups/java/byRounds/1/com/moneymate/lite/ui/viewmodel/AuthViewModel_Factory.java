package com.moneymate.lite.ui.viewmodel;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class AuthViewModel_Factory implements Factory<AuthViewModel> {
  private final Provider<Context> contextProvider;

  public AuthViewModel_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AuthViewModel get() {
    return newInstance(contextProvider.get());
  }

  public static AuthViewModel_Factory create(Provider<Context> contextProvider) {
    return new AuthViewModel_Factory(contextProvider);
  }

  public static AuthViewModel newInstance(Context context) {
    return new AuthViewModel(context);
  }
}
