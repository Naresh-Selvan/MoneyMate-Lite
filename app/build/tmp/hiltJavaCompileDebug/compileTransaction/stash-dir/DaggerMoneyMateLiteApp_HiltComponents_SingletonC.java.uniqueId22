package com.moneymate.lite;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.moneymate.lite.data.AppDatabase;
import com.moneymate.lite.data.dao.LoanDao;
import com.moneymate.lite.data.dao.LoanFileDao;
import com.moneymate.lite.data.dao.PaymentDao;
import com.moneymate.lite.data.dao.PersonDao;
import com.moneymate.lite.data.firebase.RestoreHelper;
import com.moneymate.lite.data.firebase.UploadHelper;
import com.moneymate.lite.data.repository.LoanFileRepository;
import com.moneymate.lite.data.repository.LoanRepository;
import com.moneymate.lite.data.repository.PaymentRepository;
import com.moneymate.lite.data.repository.PersonRepository;
import com.moneymate.lite.di.AppModule_ProvideDatabaseFactory;
import com.moneymate.lite.di.AppModule_ProvideLoanDaoFactory;
import com.moneymate.lite.di.AppModule_ProvideLoanFileDaoFactory;
import com.moneymate.lite.di.AppModule_ProvidePaymentDaoFactory;
import com.moneymate.lite.di.AppModule_ProvidePersonDaoFactory;
import com.moneymate.lite.ui.viewmodel.AuthViewModel;
import com.moneymate.lite.ui.viewmodel.AuthViewModel_HiltModules;
import com.moneymate.lite.ui.viewmodel.LoanFileViewModel;
import com.moneymate.lite.ui.viewmodel.LoanFileViewModel_HiltModules;
import com.moneymate.lite.ui.viewmodel.LoanViewModel;
import com.moneymate.lite.ui.viewmodel.LoanViewModel_HiltModules;
import com.moneymate.lite.ui.viewmodel.PaymentViewModel;
import com.moneymate.lite.ui.viewmodel.PaymentViewModel_HiltModules;
import com.moneymate.lite.ui.viewmodel.PersonViewModel;
import com.moneymate.lite.ui.viewmodel.PersonViewModel_HiltModules;
import com.moneymate.lite.ui.viewmodel.RecentlyDeletedViewModel;
import com.moneymate.lite.ui.viewmodel.RecentlyDeletedViewModel_HiltModules;
import com.moneymate.lite.ui.viewmodel.SettingsViewModel;
import com.moneymate.lite.ui.viewmodel.SettingsViewModel_HiltModules;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.IdentifierNameString;
import dagger.internal.KeepFieldType;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

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
public final class DaggerMoneyMateLiteApp_HiltComponents_SingletonC {
  private DaggerMoneyMateLiteApp_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public MoneyMateLiteApp_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements MoneyMateLiteApp_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public MoneyMateLiteApp_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements MoneyMateLiteApp_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public MoneyMateLiteApp_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements MoneyMateLiteApp_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public MoneyMateLiteApp_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements MoneyMateLiteApp_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public MoneyMateLiteApp_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements MoneyMateLiteApp_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public MoneyMateLiteApp_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements MoneyMateLiteApp_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public MoneyMateLiteApp_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements MoneyMateLiteApp_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public MoneyMateLiteApp_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends MoneyMateLiteApp_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends MoneyMateLiteApp_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends MoneyMateLiteApp_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends MoneyMateLiteApp_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(ImmutableMap.<String, Boolean>builderWithExpectedSize(7).put(LazyClassKeyProvider.com_moneymate_lite_ui_viewmodel_AuthViewModel, AuthViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_moneymate_lite_ui_viewmodel_LoanFileViewModel, LoanFileViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_moneymate_lite_ui_viewmodel_LoanViewModel, LoanViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_moneymate_lite_ui_viewmodel_PaymentViewModel, PaymentViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_moneymate_lite_ui_viewmodel_PersonViewModel, PersonViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_moneymate_lite_ui_viewmodel_RecentlyDeletedViewModel, RecentlyDeletedViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_moneymate_lite_ui_viewmodel_SettingsViewModel, SettingsViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_moneymate_lite_ui_viewmodel_LoanFileViewModel = "com.moneymate.lite.ui.viewmodel.LoanFileViewModel";

      static String com_moneymate_lite_ui_viewmodel_AuthViewModel = "com.moneymate.lite.ui.viewmodel.AuthViewModel";

      static String com_moneymate_lite_ui_viewmodel_SettingsViewModel = "com.moneymate.lite.ui.viewmodel.SettingsViewModel";

      static String com_moneymate_lite_ui_viewmodel_LoanViewModel = "com.moneymate.lite.ui.viewmodel.LoanViewModel";

      static String com_moneymate_lite_ui_viewmodel_PersonViewModel = "com.moneymate.lite.ui.viewmodel.PersonViewModel";

      static String com_moneymate_lite_ui_viewmodel_RecentlyDeletedViewModel = "com.moneymate.lite.ui.viewmodel.RecentlyDeletedViewModel";

      static String com_moneymate_lite_ui_viewmodel_PaymentViewModel = "com.moneymate.lite.ui.viewmodel.PaymentViewModel";

      @KeepFieldType
      LoanFileViewModel com_moneymate_lite_ui_viewmodel_LoanFileViewModel2;

      @KeepFieldType
      AuthViewModel com_moneymate_lite_ui_viewmodel_AuthViewModel2;

      @KeepFieldType
      SettingsViewModel com_moneymate_lite_ui_viewmodel_SettingsViewModel2;

      @KeepFieldType
      LoanViewModel com_moneymate_lite_ui_viewmodel_LoanViewModel2;

      @KeepFieldType
      PersonViewModel com_moneymate_lite_ui_viewmodel_PersonViewModel2;

      @KeepFieldType
      RecentlyDeletedViewModel com_moneymate_lite_ui_viewmodel_RecentlyDeletedViewModel2;

      @KeepFieldType
      PaymentViewModel com_moneymate_lite_ui_viewmodel_PaymentViewModel2;
    }
  }

  private static final class ViewModelCImpl extends MoneyMateLiteApp_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<AuthViewModel> authViewModelProvider;

    private Provider<LoanFileViewModel> loanFileViewModelProvider;

    private Provider<LoanViewModel> loanViewModelProvider;

    private Provider<PaymentViewModel> paymentViewModelProvider;

    private Provider<PersonViewModel> personViewModelProvider;

    private Provider<RecentlyDeletedViewModel> recentlyDeletedViewModelProvider;

    private Provider<SettingsViewModel> settingsViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.authViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.loanFileViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.loanViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.paymentViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.personViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.recentlyDeletedViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.settingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(ImmutableMap.<String, javax.inject.Provider<ViewModel>>builderWithExpectedSize(7).put(LazyClassKeyProvider.com_moneymate_lite_ui_viewmodel_AuthViewModel, ((Provider) authViewModelProvider)).put(LazyClassKeyProvider.com_moneymate_lite_ui_viewmodel_LoanFileViewModel, ((Provider) loanFileViewModelProvider)).put(LazyClassKeyProvider.com_moneymate_lite_ui_viewmodel_LoanViewModel, ((Provider) loanViewModelProvider)).put(LazyClassKeyProvider.com_moneymate_lite_ui_viewmodel_PaymentViewModel, ((Provider) paymentViewModelProvider)).put(LazyClassKeyProvider.com_moneymate_lite_ui_viewmodel_PersonViewModel, ((Provider) personViewModelProvider)).put(LazyClassKeyProvider.com_moneymate_lite_ui_viewmodel_RecentlyDeletedViewModel, ((Provider) recentlyDeletedViewModelProvider)).put(LazyClassKeyProvider.com_moneymate_lite_ui_viewmodel_SettingsViewModel, ((Provider) settingsViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return ImmutableMap.<Class<?>, Object>of();
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_moneymate_lite_ui_viewmodel_SettingsViewModel = "com.moneymate.lite.ui.viewmodel.SettingsViewModel";

      static String com_moneymate_lite_ui_viewmodel_PersonViewModel = "com.moneymate.lite.ui.viewmodel.PersonViewModel";

      static String com_moneymate_lite_ui_viewmodel_AuthViewModel = "com.moneymate.lite.ui.viewmodel.AuthViewModel";

      static String com_moneymate_lite_ui_viewmodel_RecentlyDeletedViewModel = "com.moneymate.lite.ui.viewmodel.RecentlyDeletedViewModel";

      static String com_moneymate_lite_ui_viewmodel_LoanFileViewModel = "com.moneymate.lite.ui.viewmodel.LoanFileViewModel";

      static String com_moneymate_lite_ui_viewmodel_PaymentViewModel = "com.moneymate.lite.ui.viewmodel.PaymentViewModel";

      static String com_moneymate_lite_ui_viewmodel_LoanViewModel = "com.moneymate.lite.ui.viewmodel.LoanViewModel";

      @KeepFieldType
      SettingsViewModel com_moneymate_lite_ui_viewmodel_SettingsViewModel2;

      @KeepFieldType
      PersonViewModel com_moneymate_lite_ui_viewmodel_PersonViewModel2;

      @KeepFieldType
      AuthViewModel com_moneymate_lite_ui_viewmodel_AuthViewModel2;

      @KeepFieldType
      RecentlyDeletedViewModel com_moneymate_lite_ui_viewmodel_RecentlyDeletedViewModel2;

      @KeepFieldType
      LoanFileViewModel com_moneymate_lite_ui_viewmodel_LoanFileViewModel2;

      @KeepFieldType
      PaymentViewModel com_moneymate_lite_ui_viewmodel_PaymentViewModel2;

      @KeepFieldType
      LoanViewModel com_moneymate_lite_ui_viewmodel_LoanViewModel2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.moneymate.lite.ui.viewmodel.AuthViewModel 
          return (T) new AuthViewModel();

          case 1: // com.moneymate.lite.ui.viewmodel.LoanFileViewModel 
          return (T) new LoanFileViewModel(singletonCImpl.loanFileRepositoryProvider.get());

          case 2: // com.moneymate.lite.ui.viewmodel.LoanViewModel 
          return (T) new LoanViewModel(singletonCImpl.loanRepositoryProvider.get());

          case 3: // com.moneymate.lite.ui.viewmodel.PaymentViewModel 
          return (T) new PaymentViewModel(singletonCImpl.loanRepositoryProvider.get());

          case 4: // com.moneymate.lite.ui.viewmodel.PersonViewModel 
          return (T) new PersonViewModel(singletonCImpl.personRepositoryProvider.get(), singletonCImpl.loanFileRepositoryProvider.get());

          case 5: // com.moneymate.lite.ui.viewmodel.RecentlyDeletedViewModel 
          return (T) new RecentlyDeletedViewModel(singletonCImpl.loanFileRepositoryProvider.get(), singletonCImpl.personRepositoryProvider.get());

          case 6: // com.moneymate.lite.ui.viewmodel.SettingsViewModel 
          return (T) new SettingsViewModel(singletonCImpl.uploadHelperProvider.get(), singletonCImpl.restoreHelperProvider.get(), singletonCImpl.loanFileRepositoryProvider.get(), singletonCImpl.personRepositoryProvider.get(), singletonCImpl.loanRepositoryProvider.get(), singletonCImpl.paymentRepositoryProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends MoneyMateLiteApp_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends MoneyMateLiteApp_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends MoneyMateLiteApp_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<AppDatabase> provideDatabaseProvider;

    private Provider<LoanFileDao> provideLoanFileDaoProvider;

    private Provider<LoanFileRepository> loanFileRepositoryProvider;

    private Provider<LoanDao> provideLoanDaoProvider;

    private Provider<PaymentDao> providePaymentDaoProvider;

    private Provider<LoanRepository> loanRepositoryProvider;

    private Provider<PersonDao> providePersonDaoProvider;

    private Provider<PersonRepository> personRepositoryProvider;

    private Provider<UploadHelper> uploadHelperProvider;

    private Provider<RestoreHelper> restoreHelperProvider;

    private Provider<PaymentRepository> paymentRepositoryProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<AppDatabase>(singletonCImpl, 2));
      this.provideLoanFileDaoProvider = DoubleCheck.provider(new SwitchingProvider<LoanFileDao>(singletonCImpl, 1));
      this.loanFileRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<LoanFileRepository>(singletonCImpl, 0));
      this.provideLoanDaoProvider = DoubleCheck.provider(new SwitchingProvider<LoanDao>(singletonCImpl, 4));
      this.providePaymentDaoProvider = DoubleCheck.provider(new SwitchingProvider<PaymentDao>(singletonCImpl, 5));
      this.loanRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<LoanRepository>(singletonCImpl, 3));
      this.providePersonDaoProvider = DoubleCheck.provider(new SwitchingProvider<PersonDao>(singletonCImpl, 7));
      this.personRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<PersonRepository>(singletonCImpl, 6));
      this.uploadHelperProvider = DoubleCheck.provider(new SwitchingProvider<UploadHelper>(singletonCImpl, 8));
      this.restoreHelperProvider = DoubleCheck.provider(new SwitchingProvider<RestoreHelper>(singletonCImpl, 9));
      this.paymentRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<PaymentRepository>(singletonCImpl, 10));
    }

    @Override
    public void injectMoneyMateLiteApp(MoneyMateLiteApp moneyMateLiteApp) {
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return ImmutableSet.<Boolean>of();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.moneymate.lite.data.repository.LoanFileRepository 
          return (T) new LoanFileRepository(singletonCImpl.provideLoanFileDaoProvider.get());

          case 1: // com.moneymate.lite.data.dao.LoanFileDao 
          return (T) AppModule_ProvideLoanFileDaoFactory.provideLoanFileDao(singletonCImpl.provideDatabaseProvider.get());

          case 2: // com.moneymate.lite.data.AppDatabase 
          return (T) AppModule_ProvideDatabaseFactory.provideDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 3: // com.moneymate.lite.data.repository.LoanRepository 
          return (T) new LoanRepository(singletonCImpl.provideLoanDaoProvider.get(), singletonCImpl.providePaymentDaoProvider.get());

          case 4: // com.moneymate.lite.data.dao.LoanDao 
          return (T) AppModule_ProvideLoanDaoFactory.provideLoanDao(singletonCImpl.provideDatabaseProvider.get());

          case 5: // com.moneymate.lite.data.dao.PaymentDao 
          return (T) AppModule_ProvidePaymentDaoFactory.providePaymentDao(singletonCImpl.provideDatabaseProvider.get());

          case 6: // com.moneymate.lite.data.repository.PersonRepository 
          return (T) new PersonRepository(singletonCImpl.providePersonDaoProvider.get());

          case 7: // com.moneymate.lite.data.dao.PersonDao 
          return (T) AppModule_ProvidePersonDaoFactory.providePersonDao(singletonCImpl.provideDatabaseProvider.get());

          case 8: // com.moneymate.lite.data.firebase.UploadHelper 
          return (T) new UploadHelper();

          case 9: // com.moneymate.lite.data.firebase.RestoreHelper 
          return (T) new RestoreHelper();

          case 10: // com.moneymate.lite.data.repository.PaymentRepository 
          return (T) new PaymentRepository(singletonCImpl.providePaymentDaoProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
