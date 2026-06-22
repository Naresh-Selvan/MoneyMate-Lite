package com.moneymate.lite.data.firebase;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class UploadHelper_Factory implements Factory<UploadHelper> {
  @Override
  public UploadHelper get() {
    return newInstance();
  }

  public static UploadHelper_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static UploadHelper newInstance() {
    return new UploadHelper();
  }

  private static final class InstanceHolder {
    private static final UploadHelper_Factory INSTANCE = new UploadHelper_Factory();
  }
}
