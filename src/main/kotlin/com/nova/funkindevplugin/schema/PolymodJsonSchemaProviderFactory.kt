package com.nova.funkindevplugin.schema

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.SchemaType
import org.jetbrains.annotations.Nls

class PolymodJsonSchemaProviderFactory : JsonSchemaProviderFactory {
  override fun getProviders(p0: Project): List<JsonSchemaFileProvider?> {
    return listOf(
      object : JsonSchemaFileProvider {
        override fun getName(): @Nls String = "Polymod Metadata Schema"
        override fun isAvailable(file: VirtualFile): Boolean = file.name == "_polymod_meta.json"

        override fun getSchemaType(): SchemaType = SchemaType.embeddedSchema

        override fun getSchemaFile(): VirtualFile? {
          return JsonSchemaProviderFactory.getResourceFile(
            PolymodJsonSchemaProviderFactory::class.java,
            "/schemas/_polymod_meta.json"
          )
        }
      }
    )
  }
}