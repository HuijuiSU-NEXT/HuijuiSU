package com.huijui.sunext.di

import coil.ImageLoader
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import me.zhanghai.android.appiconloader.coil.AppIconFetcher
import me.zhanghai.android.appiconloader.coil.AppIconKeyer
import okhttp3.Cache
import okhttp3.OkHttpClient
import com.huijui.sunext.BuildConfig
import com.huijui.sunext.data.AppSettingsRepository
import com.huijui.sunext.data.application.ApplicationControlRepository
import com.huijui.sunext.data.application.DynamicManagerRepository
import com.huijui.sunext.data.count.CountRepository
import com.huijui.sunext.data.download.DownloadRepository
import com.huijui.sunext.data.file.ModuleFileRepository
import com.huijui.sunext.data.flash.FlashRepository
import com.huijui.sunext.data.kernel.KernelRepository
import com.huijui.sunext.data.kernel.UmountRepository
import com.huijui.sunext.data.logging.BugreportRepository
import com.huijui.sunext.data.logging.SulogRepository
import com.huijui.sunext.data.module.ModuleActionRepository
import com.huijui.sunext.data.module.ModuleCatalogRepository
import com.huijui.sunext.data.module.ModulePreferencesRepository
import com.huijui.sunext.data.module.ModuleRepository
import com.huijui.sunext.data.network.NetworkRequestRepository
import com.huijui.sunext.data.network.NetworkStatusRepository
import com.huijui.sunext.data.network.WebResourceRepository
import com.huijui.sunext.data.packageinfo.AppIconDataSource
import com.huijui.sunext.data.packageinfo.InstalledPackageCache
import com.huijui.sunext.data.packageinfo.InstalledPackageRepository
import com.huijui.sunext.data.packageinfo.RootServiceRepository
import com.huijui.sunext.data.packageinfo.SuperUserRepository
import com.huijui.sunext.data.profile.ProfileRepository
import com.huijui.sunext.data.profile.ProfileTemplateRepository
import com.huijui.sunext.data.settings.LocaleHelper
import com.huijui.sunext.data.settings.LocaleRepository
import com.huijui.sunext.data.settings.SettingsPlatformRepository
import com.huijui.sunext.data.shell.KsuCliRepository
import com.huijui.sunext.data.shell.ShortcutRepository
import com.huijui.sunext.data.startup.ApplicationInitializationRepository
import com.huijui.sunext.data.startup.StartupRepository
import com.huijui.sunext.data.susfs.SuSFSConfigHelper
import com.huijui.sunext.data.susfs.SuSFSRepository
import com.huijui.sunext.data.system.HomeRuntimeRepository
import com.huijui.sunext.data.system.HomeStateRepository
import com.huijui.sunext.data.text.HanziToPinyin
import com.huijui.sunext.data.theme.MonetCompatColorSource
import com.huijui.sunext.data.theme.ThemeRepository
import com.huijui.sunext.data.update.ManagerUpdateRepository
import com.huijui.sunext.data.webui.WebUiRepository
import com.huijui.sunext.domain.text.TextTransliterator
import com.huijui.sunext.domain.usecase.AddUmountPathUseCase
import com.huijui.sunext.domain.usecase.ApplyLanguageUseCase
import com.huijui.sunext.domain.usecase.BackupAllowlistUseCase
import com.huijui.sunext.domain.usecase.CalculateInstalledModuleSizeUseCase
import com.huijui.sunext.domain.usecase.CheckFlashModuleMountUseCase
import com.huijui.sunext.domain.usecase.CheckManagerUpdateUseCase
import com.huijui.sunext.domain.usecase.CleanSulogUseCase
import com.huijui.sunext.domain.usecase.ClearDynamicManagerUseCase
import com.huijui.sunext.domain.usecase.ConfigureSuLogUseCase
import com.huijui.sunext.domain.usecase.ControlAppUseCase
import com.huijui.sunext.domain.usecase.DeleteProfileTemplateUseCase
import com.huijui.sunext.domain.usecase.EnableSulogUseCase
import com.huijui.sunext.domain.usecase.EnqueueDownloadUseCase
import com.huijui.sunext.domain.usecase.EnqueueManagerUpdateUseCase
import com.huijui.sunext.domain.usecase.EnsureManagerInstalledUseCase
import com.huijui.sunext.domain.usecase.ExecuteFlashOperationUseCase
import com.huijui.sunext.domain.usecase.ExecuteModuleActionUseCase
import com.huijui.sunext.domain.usecase.ExportProfileTemplatesUseCase
import com.huijui.sunext.domain.usecase.ExtractModuleIdUseCase
import com.huijui.sunext.domain.usecase.ExtractModuleNameUseCase
import com.huijui.sunext.domain.usecase.FetchRemoteTextUseCase
import com.huijui.sunext.domain.usecase.GenerateBugreportUseCase
import com.huijui.sunext.domain.usecase.GetAppProfileUseCase
import com.huijui.sunext.domain.usecase.GetAppSepolicyUseCase
import com.huijui.sunext.domain.usecase.GetBooleanPreferenceUseCase
import com.huijui.sunext.domain.usecase.GetCatalogModuleUseCase
import com.huijui.sunext.domain.usecase.GetDefaultUmountModulesUseCase
import com.huijui.sunext.domain.usecase.GetHomeBasicInfoUseCase
import com.huijui.sunext.domain.usecase.GetInstallEnvironmentUseCase
import com.huijui.sunext.domain.usecase.GetKernelFeatureSettingsUseCase
import com.huijui.sunext.domain.usecase.GetKernelStatusUseCase
import com.huijui.sunext.domain.usecase.GetManagerRuntimeInfoUseCase
import com.huijui.sunext.domain.usecase.GetPlatformFeatureStatusUseCase
import com.huijui.sunext.domain.usecase.GetProfileTemplateUseCase
import com.huijui.sunext.domain.usecase.GetStringPreferenceUseCase
import com.huijui.sunext.domain.usecase.GetStringSetPreferenceUseCase
import com.huijui.sunext.domain.usecase.GetSuSFSStatusUseCase
import com.huijui.sunext.domain.usecase.GetSuperUserAppGroupUseCase
import com.huijui.sunext.domain.usecase.ImportAllowlistUseCase
import com.huijui.sunext.domain.usecase.ImportProfileTemplatesUseCase
import com.huijui.sunext.domain.usecase.InitializeApplicationUseCase
import com.huijui.sunext.domain.usecase.IsLateLoadModeUseCase
import com.huijui.sunext.domain.usecase.IsModuleUriAccessibleUseCase
import com.huijui.sunext.domain.usecase.IsNetworkAvailableUseCase
import com.huijui.sunext.domain.usecase.IsSoftRebootPreferredUseCase
import com.huijui.sunext.domain.usecase.IsSystemLanguageSettingsUseCase
import com.huijui.sunext.domain.usecase.LaunchSystemLanguageSettingsUseCase
import com.huijui.sunext.domain.usecase.LoadSettingsPlatformUseCase
import com.huijui.sunext.domain.usecase.ObserveCatalogModulesUseCase
import com.huijui.sunext.domain.usecase.ObserveDownloadUseCase
import com.huijui.sunext.domain.usecase.ObserveDynamicManagerStateUseCase
import com.huijui.sunext.domain.usecase.ObserveInstalledModulesUseCase
import com.huijui.sunext.domain.usecase.ObserveKernelFlashUseCase
import com.huijui.sunext.domain.usecase.ObserveModuleCatalogOfflineUseCase
import com.huijui.sunext.domain.usecase.ObserveModuleCatalogRefreshingUseCase
import com.huijui.sunext.domain.usecase.ObserveProfileTemplateOfflineUseCase
import com.huijui.sunext.domain.usecase.ObserveProfileTemplateRefreshingUseCase
import com.huijui.sunext.domain.usecase.ObserveProfileTemplatesUseCase
import com.huijui.sunext.domain.usecase.ObserveStartupStateUseCase
import com.huijui.sunext.domain.usecase.ObserveSulogStateUseCase
import com.huijui.sunext.domain.usecase.ObserveSuperUserStateUseCase
import com.huijui.sunext.domain.usecase.ObserveUmountStateUseCase
import com.huijui.sunext.domain.usecase.RebootUseCase
import com.huijui.sunext.domain.usecase.RefreshDynamicManagerUseCase
import com.huijui.sunext.domain.usecase.RefreshInstalledModulesUseCase
import com.huijui.sunext.domain.usecase.RefreshModuleCatalogUseCase
import com.huijui.sunext.domain.usecase.RefreshProfileTemplatesUseCase
import com.huijui.sunext.domain.usecase.RefreshSulogUseCase
import com.huijui.sunext.domain.usecase.RefreshSuperUsersUseCase
import com.huijui.sunext.domain.usecase.RefreshUmountPathsUseCase
import com.huijui.sunext.domain.usecase.RemovePreferenceUseCase
import com.huijui.sunext.domain.usecase.RemoveUmountPathUseCase
import com.huijui.sunext.domain.usecase.SaveModuleActionLogUseCase
import com.huijui.sunext.domain.usecase.SaveProfileTemplateUseCase
import com.huijui.sunext.domain.usecase.SelectDynamicManagerUseCase
import com.huijui.sunext.domain.usecase.SetAppProfileUseCase
import com.huijui.sunext.domain.usecase.SetAppSepolicyUseCase
import com.huijui.sunext.domain.usecase.SetBooleanPreferenceUseCase
import com.huijui.sunext.domain.usecase.SetDefaultUmountModulesUseCase
import com.huijui.sunext.domain.usecase.SetKernelUmountEnabledUseCase
import com.huijui.sunext.domain.usecase.SetManualDynamicManagerUseCase
import com.huijui.sunext.domain.usecase.SetModuleEnabledUseCase
import com.huijui.sunext.domain.usecase.SetModuleRemovedUseCase
import com.huijui.sunext.domain.usecase.SetSelinuxHideEnabledUseCase
import com.huijui.sunext.domain.usecase.SetStringPreferenceUseCase
import com.huijui.sunext.domain.usecase.SetStringSetPreferenceUseCase
import com.huijui.sunext.domain.usecase.SetSuEnabledUseCase
import com.huijui.sunext.domain.usecase.StartKernelFlashUseCase
import com.huijui.sunext.domain.usecase.SuSFSConfigUseCase
import com.huijui.sunext.domain.usecase.TakeModuleUriPermissionUseCase
import com.huijui.sunext.domain.usecase.TransliterateTextUseCase
import com.huijui.sunext.domain.usecase.UpdateAppearanceUseCase
import com.huijui.sunext.domain.usecase.UpdateCachedModuleEnabledUseCase
import com.huijui.sunext.domain.usecase.UpdatePlatformSettingUseCase
import com.huijui.sunext.domain.usecase.ValidateSepolicyUseCase
import com.huijui.sunext.ui.activity.util.ThemeUtils
import com.huijui.sunext.ui.component.ZipFileDetector
import com.huijui.sunext.ui.theme.BackgroundManager
import com.huijui.sunext.ui.theme.CardConfig
import com.huijui.sunext.ui.theme.ThemeConfig
import com.huijui.sunext.ui.util.Shortcut
import com.huijui.sunext.ui.viewmodel.AppProfileViewModel
import com.huijui.sunext.ui.viewmodel.DynamicManagerViewModel
import com.huijui.sunext.ui.viewmodel.ExecuteModuleActionViewModel
import com.huijui.sunext.ui.viewmodel.FlashViewModel
import com.huijui.sunext.ui.viewmodel.HomeViewModel
import com.huijui.sunext.ui.viewmodel.InstallViewModel
import com.huijui.sunext.ui.viewmodel.KernelFlashViewModel
import com.huijui.sunext.ui.viewmodel.MainIntentViewModel
import com.huijui.sunext.ui.viewmodel.ModuleDetailViewModel
import com.huijui.sunext.ui.viewmodel.ModuleRepoViewModel
import com.huijui.sunext.ui.viewmodel.ModuleViewModel
import com.huijui.sunext.ui.viewmodel.SettingsViewModel
import com.huijui.sunext.ui.viewmodel.SuSFSViewModel
import com.huijui.sunext.ui.viewmodel.SulogViewModel
import com.huijui.sunext.ui.viewmodel.SuperUserViewModel
import com.huijui.sunext.ui.viewmodel.TemplateEditorViewModel
import com.huijui.sunext.ui.viewmodel.TemplateViewModel
import com.huijui.sunext.ui.viewmodel.UmountManagerScreenViewModel
import com.huijui.sunext.ui.webui.MonetColorsProvider
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val applicationScopeQualifier = named("applicationScope")

val coreModule = module {
    single<CoroutineScope>(applicationScopeQualifier) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
    single {
        OkHttpClient.Builder()
            .cache(Cache(File(androidApplication().cacheDir, "okhttp"), 10L * 1024L * 1024L))
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "HuiJuiSU_Next/${BuildConfig.VERSION_CODE}")
                        .header("Accept-Language", Locale.getDefault().toLanguageTag())
                        .build(),
                )
            }
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()
    }
    single {
        val application = androidApplication()
        val iconSize = application.resources.getDimensionPixelSize(android.R.dimen.app_icon_size)
        ImageLoader.Builder(application)
            .components {
                add(AppIconKeyer())
                add(AppIconFetcher.Factory(iconSize, false, application))
            }
            .build()
    }
}

val repositoryModule = module {
    single { KsuCliRepository(androidApplication()) }
    singleOf(::CountRepository)
    singleOf(::InstalledPackageCache)
    singleOf(::AppIconDataSource)
    singleOf(::RootServiceRepository)
    singleOf(::InstalledPackageRepository)
    single {
        SuperUserRepository(
            application = get(),
            cache = get(),
            installedPackageRepository = get(),
            profileRepository = get(),
            applicationScope = get(applicationScopeQualifier),
        )
    }
    single {
        AppSettingsRepository(
            ctx = androidApplication(),
            applicationScope = get(applicationScopeQualifier),
        )
    }
    singleOf(::StartupRepository)
    single {
        ApplicationInitializationRepository(
            application = get(),
            imageLoader = get(),
            applicationScope = get(applicationScopeQualifier),
            flashRepository = get(),
            ksuCliRepository = get(),
            monetCompatColorSource = get(),
        )
    }
    singleOf(::ManagerUpdateRepository)
    singleOf(::ApplicationControlRepository)
    singleOf(::DownloadRepository)
    single { FlashRepository(get(), get(applicationScopeQualifier), get(), get()) }
    singleOf(::KernelRepository)
    singleOf(::HomeRuntimeRepository)
    singleOf(::HomeStateRepository)
    singleOf(::NetworkStatusRepository)
    singleOf(::NetworkRequestRepository)
    singleOf(::DynamicManagerRepository)
    singleOf(::SulogRepository)
    singleOf(::BugreportRepository)
    singleOf(::UmountRepository)
    singleOf(::ModuleCatalogRepository)
    singleOf(::ModuleRepository)
    singleOf(::ModulePreferencesRepository)
    singleOf(::ModuleActionRepository)
    singleOf(::WebResourceRepository)
    singleOf(::WebUiRepository)
    singleOf(::ModuleFileRepository)
    singleOf(::ProfileRepository)
    singleOf(::ProfileTemplateRepository)
    singleOf(::SuSFSConfigHelper)
    singleOf(::SuSFSRepository)
    singleOf(::MonetCompatColorSource)
    singleOf(::ThemeRepository)
    single {
        val themeRepository = get<ThemeRepository>()
        ThemeConfig(themeRepository::defaultSeedColor)
    }
    singleOf(::CardConfig)
    singleOf(::BackgroundManager)
    singleOf(::ThemeUtils)
    singleOf(::LocaleHelper)
    singleOf(::LocaleRepository)
    singleOf(::SettingsPlatformRepository)
    singleOf(::ShortcutRepository)
    singleOf(::Shortcut)
    singleOf(::MonetColorsProvider)
    singleOf(::ZipFileDetector)
    single { HanziToPinyin.create() } bind TextTransliterator::class
}

val useCaseModule = module {
    factoryOf(::InitializeApplicationUseCase)
    factoryOf(::GetHomeBasicInfoUseCase)
    factoryOf(::IsNetworkAvailableUseCase)
    factoryOf(::LoadSettingsPlatformUseCase)
    factoryOf(::UpdateAppearanceUseCase)
    factoryOf(::UpdatePlatformSettingUseCase)
    factoryOf(::GetPlatformFeatureStatusUseCase)
    factoryOf(::IsSoftRebootPreferredUseCase)
    factoryOf(::CheckManagerUpdateUseCase)
    factoryOf(::EnsureManagerInstalledUseCase)
    factoryOf(::RebootUseCase)
    factoryOf(::EnqueueDownloadUseCase)
    factoryOf(::EnqueueManagerUpdateUseCase)
    factoryOf(::ObserveDownloadUseCase)
    factoryOf(::GetKernelStatusUseCase)
    factoryOf(::GetInstallEnvironmentUseCase)
    factoryOf(::ExecuteFlashOperationUseCase)
    factoryOf(::CheckFlashModuleMountUseCase)
    factoryOf(::GetManagerRuntimeInfoUseCase)
    factoryOf(::GetKernelFeatureSettingsUseCase)
    factoryOf(::SetSuEnabledUseCase)
    factoryOf(::SetKernelUmountEnabledUseCase)
    factoryOf(::ConfigureSuLogUseCase)
    factoryOf(::SetSelinuxHideEnabledUseCase)
    factoryOf(::SetDefaultUmountModulesUseCase)
    factoryOf(::IsLateLoadModeUseCase)
    factoryOf(::GetAppProfileUseCase)
    factoryOf(::SetAppProfileUseCase)
    factoryOf(::GetAppSepolicyUseCase)
    factoryOf(::SetAppSepolicyUseCase)
    factoryOf(::ControlAppUseCase)
    factoryOf(::ValidateSepolicyUseCase)
    factoryOf(::GetDefaultUmountModulesUseCase)
    factoryOf(::GetSuSFSStatusUseCase)
    factoryOf(::SuSFSConfigUseCase)
    factoryOf(::ApplyLanguageUseCase)
    factoryOf(::IsSystemLanguageSettingsUseCase)
    factoryOf(::LaunchSystemLanguageSettingsUseCase)
    factoryOf(::GenerateBugreportUseCase)
    factoryOf(::ObserveStartupStateUseCase)
    factoryOf(::GetSuperUserAppGroupUseCase)
    factoryOf(::ObserveCatalogModulesUseCase)
    factoryOf(::ObserveModuleCatalogRefreshingUseCase)
    factoryOf(::ObserveModuleCatalogOfflineUseCase)
    factoryOf(::RefreshModuleCatalogUseCase)
    factoryOf(::GetCatalogModuleUseCase)
    factoryOf(::ObserveProfileTemplatesUseCase)
    factoryOf(::ObserveProfileTemplateRefreshingUseCase)
    factoryOf(::ObserveProfileTemplateOfflineUseCase)
    factoryOf(::RefreshProfileTemplatesUseCase)
    factoryOf(::GetProfileTemplateUseCase)
    factoryOf(::SaveProfileTemplateUseCase)
    factoryOf(::DeleteProfileTemplateUseCase)
    factoryOf(::ImportProfileTemplatesUseCase)
    factoryOf(::ExportProfileTemplatesUseCase)
    factoryOf(::GetBooleanPreferenceUseCase)
    factoryOf(::SetBooleanPreferenceUseCase)
    factoryOf(::GetStringPreferenceUseCase)
    factoryOf(::SetStringPreferenceUseCase)
    factoryOf(::GetStringSetPreferenceUseCase)
    factoryOf(::SetStringSetPreferenceUseCase)
    factoryOf(::ObserveDynamicManagerStateUseCase)
    factoryOf(::RefreshDynamicManagerUseCase)
    factoryOf(::SelectDynamicManagerUseCase)
    factoryOf(::SetManualDynamicManagerUseCase)
    factoryOf(::ClearDynamicManagerUseCase)
    factoryOf(::ObserveSulogStateUseCase)
    factoryOf(::RefreshSulogUseCase)
    factoryOf(::EnableSulogUseCase)
    factoryOf(::CleanSulogUseCase)
    factoryOf(::ObserveUmountStateUseCase)
    factoryOf(::RefreshUmountPathsUseCase)
    factoryOf(::AddUmountPathUseCase)
    factoryOf(::RemoveUmountPathUseCase)
    factoryOf(::ObserveKernelFlashUseCase)
    factoryOf(::StartKernelFlashUseCase)
    factoryOf(::RemovePreferenceUseCase)
    factoryOf(::ObserveSuperUserStateUseCase)
    factoryOf(::RefreshSuperUsersUseCase)
    factoryOf(::BackupAllowlistUseCase)
    factoryOf(::ImportAllowlistUseCase)
    factoryOf(::FetchRemoteTextUseCase)
    factoryOf(::IsModuleUriAccessibleUseCase)
    factoryOf(::TakeModuleUriPermissionUseCase)
    factoryOf(::ExtractModuleNameUseCase)
    factoryOf(::ExtractModuleIdUseCase)
    factoryOf(::ObserveInstalledModulesUseCase)
    factoryOf(::RefreshInstalledModulesUseCase)
    factoryOf(::CalculateInstalledModuleSizeUseCase)
    factoryOf(::UpdateCachedModuleEnabledUseCase)
    factoryOf(::ExecuteModuleActionUseCase)
    factoryOf(::SaveModuleActionLogUseCase)
    factoryOf(::SetModuleEnabledUseCase)
    factoryOf(::SetModuleRemovedUseCase)
    factoryOf(::TransliterateTextUseCase)
}

val viewModelModule = module {
    viewModel { parameters ->
        AppProfileViewModel(
            uid = parameters[0],
            packageName = parameters[1],
            getAppGroup = get(),
            getProfile = get(),
            getDefaultUmountModules = get(),
            setProfile = get(),
            getSepolicy = get(),
            setSepolicy = get(),
            controlApp = get(),
            validateSepolicy = get(),
        )
    }
    viewModelOf(::HomeViewModel)
    viewModelOf(::InstallViewModel)
    viewModelOf(::MainIntentViewModel)
    viewModelOf(::KernelFlashViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::ModuleViewModel)
    viewModelOf(::SuperUserViewModel)
    viewModelOf(::SuSFSViewModel)
    viewModelOf(::ModuleRepoViewModel)
    viewModel { parameters -> ModuleDetailViewModel(parameters[0], get()) }
    viewModelOf(::TemplateViewModel)
    viewModel { parameters ->
        TemplateEditorViewModel(
            templateId = parameters[0],
            readOnly = parameters[1],
            isCreation = parameters[2],
            getTemplate = get(),
            saveTemplate = get(),
            deleteTemplate = get(),
        )
    }
    viewModelOf(::SulogViewModel)
    viewModelOf(::DynamicManagerViewModel)
    viewModelOf(::FlashViewModel)
    viewModelOf(::UmountManagerScreenViewModel)
    viewModel { parameters ->
        ExecuteModuleActionViewModel(
            moduleId = parameters[0],
            executeModuleAction = get(),
            saveModuleActionLog = get(),
        )
    }
}

val appModules = listOf(coreModule, repositoryModule, useCaseModule, viewModelModule)
