package com.nova.funkindevplugin.build

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VfsUtil
import com.nova.funkindevplugin.libraries.LibraryConfig
import com.nova.funkindevplugin.libraries.VSliceLibraryManager
import com.nova.funkindevplugin.libraries.VSliceLibraryType
import java.io.File

class VSliceLibrarySetup : ProjectActivity {
  private val LOG = logger<VSliceLibrarySetup>()

  companion object {
    const val LIBRARY_NAME = "VSlice Modding Libraries"
  }

  override suspend fun execute(project: Project) {
    val globalCache = File(System.getProperty("user.home"), ".vslice_libs_cache")

    // Only setup if cache exists (don't download on startup)
    if (!globalCache.exists()) {
      LOG.info("Library cache doesn't exist yet - use 'Download VSlice Libraries' action")
      return
    }

    val hasHaxeModule = ModuleManager.getInstance(project).modules.any { module ->
      ModuleRootManager.getInstance(module).module.moduleTypeName == "HAXE_MODULE"
    }

    if (!hasHaxeModule) {
      LOG.warn("No Haxe module found in project")
      return
    }

    runSetup(project)
  }

  fun runSetup(project: Project) {
    val globalCache = VSliceLibraryManager.getGlobalCache()
    val config = VSliceLibraryManager.loadConfigOrShowError(project) ?: return

    ApplicationManager.getApplication().invokeLater {
      setupLibraries(project, globalCache, config)
    }
  }

  private fun setupLibraries(project: Project, globalCache: File, config: LibraryConfig) {
    ApplicationManager.getApplication().runWriteAction {
      if (project.isDisposed) return@runWriteAction

      val libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(project)
      val tableModel = libraryTable.modifiableModel

      var library = tableModel.getLibraryByName(LIBRARY_NAME)
      if (library == null) library = tableModel.createLibrary(LIBRARY_NAME, VSliceLibraryType.VSLICE_KIND)

      val libModel = library.modifiableModel

      // Clear existing roots
      libModel.getUrls(OrderRootType.CLASSES).forEach { libModel.removeRoot(it, OrderRootType.CLASSES) }
      libModel.getUrls(OrderRootType.SOURCES).forEach { libModel.removeRoot(it, OrderRootType.SOURCES) }

      // Add library source roots
      config.libraries.keys.forEach { libName ->
        val libDir = File(globalCache, libName)
        if (libDir.exists()) {
          VfsUtil.markDirtyAndRefresh(false, true, true, libDir)
          val vFile = VfsUtil.findFileByIoFile(libDir, true)
          if (vFile != null) {
            // Adding each individual lib folder as a root
            libModel.addRoot(vFile.url, OrderRootType.CLASSES)
            libModel.addRoot(vFile.url, OrderRootType.SOURCES)
            LOG.info("Added root for $libName: ${vFile.url}")
          }
        } else {
          LOG.warn("Library directory missing for $libName at ${libDir.absolutePath}")
        }
      }

      libModel.commit()
      tableModel.commit()

      // Attach to Haxe modules
      ModuleManager.getInstance(project).modules
        .filter { it.moduleTypeName == "HAXE_MODULE" }
        .forEach { module ->
          val rootModel = ModuleRootManager.getInstance(module).modifiableModel
          val hasLib = rootModel.orderEntries.any {
            it is com.intellij.openapi.roots.LibraryOrderEntry && it.libraryName == LIBRARY_NAME
          }
          if (!hasLib) rootModel.addLibraryEntry(library)
          rootModel.commit()
        }

      LOG.info("VSlice libraries setup complete")
    }
  }
}