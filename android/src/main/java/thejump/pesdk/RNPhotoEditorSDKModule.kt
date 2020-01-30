package thejump.pesdk

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener
import ly.img.android.IMGLY
import ly.img.android.PESDK
import ly.img.android.pesdk.PhotoEditorSettingsList
import ly.img.android.pesdk.backend.decoder.ImageSource
import ly.img.android.pesdk.backend.model.config.CropAspectAsset
import ly.img.android.pesdk.backend.model.state.AssetConfig
import ly.img.android.pesdk.backend.model.state.LoadSettings
import ly.img.android.pesdk.backend.model.state.SaveSettings
import ly.img.android.pesdk.backend.model.state.manager.SettingsList
import ly.img.android.pesdk.kotlin_extension.continueWithExceptions
import ly.img.android.pesdk.ui.activity.ImgLyIntent
import ly.img.android.pesdk.ui.activity.PhotoEditorBuilder
import ly.img.android.pesdk.ui.model.state.UiConfigMainMenu
import ly.img.android.pesdk.ui.panels.item.ToolItem
import ly.img.android.pesdk.ui.utils.PermissionRequest
import ly.img.android.pesdk.utils.MainThreadRunnable
import ly.img.android.pesdk.utils.SequenceRunnable
import ly.img.android.pesdk.utils.UriHelper
import ly.img.android.sdk.config.*
import ly.img.android.serializer._3._0._0.PESDKFileReader
import ly.img.android.serializer._3._0._0.PESDKFileWriter
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import kotlin.math.roundToInt


class RNPhotoEditorSDKModule(val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), ActivityEventListener, PermissionListener {

  companion object {
    // This number must be unique. It is public to allow client code to change it if the same value is used elsewhere.
    var EDITOR_RESULT_ID = 29064
  }

  init {
    reactContext.addActivityEventListener(this)

  }

  private var currentSettingsList: PhotoEditorSettingsList? = null
  private var currentPromise: Promise? = null
  private var currentConfig: Configuration? = null

  @ReactMethod
  fun unlockWithLicense(license: String) {
    PESDK.initSDKWithLicenseData(license)
    IMGLY.authorize()
  }

  override fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, resultData: Intent?) {
    val data = resultData ?: return // If resultData is null the result is not from us.
    when (requestCode) {
      EDITOR_RESULT_ID -> {
        when (resultCode) {
          Activity.RESULT_CANCELED -> {
            currentPromise?.resolve(null)
          }
          Activity.RESULT_OK -> {
            SequenceRunnable("Export Done") {
              val sourcePath = data.getParcelableExtra<Uri>(ImgLyIntent.SOURCE_IMAGE_URI)
              val resultPath = data.getParcelableExtra<Uri>(ImgLyIntent.RESULT_IMAGE_URI)

              val serializationConfig = currentConfig?.export?.serialization
              val settingsList = data.getParcelableExtra<SettingsList>(ImgLyIntent.SETTINGS_LIST)

              val serialization: Any? = if (serializationConfig?.enabled == true) {
                skipIfNotExists {
                  settingsList.let { settingsList ->
                    if (serializationConfig.embedSourceImage == true) {
                      Log.i("ImgLySdk", "EmbedSourceImage is currently not supported by the Android SDK")
                    }
                    when (serializationConfig.exportType) {
                      SerializationExportType.FILE_URL -> {
                        val file = serializationConfig.filename?.let { Export.convertPathToFile(it) }
                          ?: File.createTempFile("serialization", ".json")
                        PESDKFileWriter(settingsList).writeJson(file)
                        file.absolutePath
                      }
                      SerializationExportType.OBJECT -> {
                        ReactJSON.convertJsonToMap(
                          JSONObject(
                            PESDKFileWriter(settingsList).writeJsonAsString()
                          )
                        ) as Any?
                      }
                    }
                  }
                } ?: run {
                  Log.i("ImgLySdk", "You need to include 'backend:serializer' Module, to use serialisation!")
                  null
                }
              } else {
                null
              }

              val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
              }

              //jgc start
   BitmapFactory.decodeFile( resultPath?.path, options)
   val height = options.outHeight
   val width = options.outWidth


              currentPromise?.resolve(
                reactMap(
                  "image" to when (currentConfig?.export?.image?.exportType) {
                    ImageExportType.FILE_URL -> resultPath?.let {
                      val imageSource = ImageSource.create(it)
                      "data:${imageSource.imageFormat.mimeType};base64,${imageSource.asBase64}"
                    }
                    ImageExportType.DATA_URL -> resultPath?.toString()
                    else -> resultPath?.toString()
                  },
                  "width" to width,
                  "height" to height,
                  "hasChanges" to (sourcePath?.path != resultPath?.path),
                  "serialization" to serialization
                )
              )
            }()
          }
        }
      }
      //jgc end

    }
  }


  override fun onNewIntent(intent: Intent?) {
  }

  @ReactMethod
  fun present(image: String?, config: ReadableMap?, serialization: String?, promise: Promise) {
    IMGLY.authorize()

    val settingsList = PhotoEditorSettingsList()

    currentSettingsList = settingsList
    currentConfig = ConfigLoader.readFrom(config?.toHashMap() ?: mapOf()).also {
      it.applyOn(settingsList)
    }
    currentPromise = promise

    settingsList.configure<LoadSettings> { loadSettings ->
      image?.also {
        if (it.startsWith("data:")) {
          loadSettings.setSource(UriHelper.createFromBase64String(it.substringAfter("base64,")), deleteProtectedSource = false)
        } else {
          val potentialFile = continueWithExceptions { File(it) }
          if (potentialFile?.exists() == true) {
            loadSettings.setSource(Uri.fromFile(potentialFile), deleteProtectedSource = true)
          } else {
            loadSettings.setSource(Uri.parse(it), deleteProtectedSource = true)
          }
        }
      }
    }.configure<SaveSettings> {
      it.savePolicy = SaveSettings.SavePolicy.RETURN_SOURCE_OR_CREATE_OUTPUT_IF_NECESSARY
    }


    //jgc start changes
    if(config!=null) {
      if (config.toHashMap()["textFirst"] == true) {
        settingsList.getSettingsModel(UiConfigMainMenu::class.java).apply {
          // Set the tools you want keep sure you licence is cover the feature and do not forget to include the correct modules in your build.gradle
          setToolList(
            ToolItem("imgly_tool_text_design", R.string.pesdk_textDesign_title_name, ImageSource.create(R.drawable.imgly_icon_tool_text_design)),
            ToolItem("imgly_tool_filter", R.string.pesdk_filter_title_name, ImageSource.create(R.drawable.imgly_icon_tool_filters)),
            ToolItem("imgly_tool_transform", R.string.pesdk_transform_title_name, ImageSource.create(R.drawable.imgly_icon_tool_transform)),
            ToolItem("imgly_tool_adjustment", R.string.pesdk_adjustments_title_name, ImageSource.create(R.drawable.imgly_icon_tool_adjust)),
            ToolItem("imgly_tool_sticker_selection", R.string.pesdk_sticker_title_name, ImageSource.create(R.drawable.imgly_icon_tool_sticker)),
            ToolItem("imgly_tool_text", R.string.pesdk_text_title_name, ImageSource.create(R.drawable.imgly_icon_tool_text)),
            ToolItem("imgly_tool_overlay", R.string.pesdk_overlay_title_name, ImageSource.create(R.drawable.imgly_icon_tool_overlay)),
            ToolItem("imgly_tool_frame", R.string.pesdk_frame_title_name, ImageSource.create(R.drawable.imgly_icon_tool_frame)),
            ToolItem("imgly_tool_brush", R.string.pesdk_brush_title_name, ImageSource.create(R.drawable.imgly_icon_tool_brush)),
            ToolItem("imgly_tool_focus", R.string.pesdk_focus_title_name, ImageSource.create(R.drawable.imgly_icon_tool_focus))
          )
        }
      } else {
        settingsList.getSettingsModel(UiConfigMainMenu::class.java).apply {
          // Set the tools you want keep sure you licence is cover the feature and do not forget to include the correct modules in your build.gradle
          setToolList(
            ToolItem("imgly_tool_filter", R.string.pesdk_filter_title_name, ImageSource.create(R.drawable.imgly_icon_tool_filters)),
            ToolItem("imgly_tool_text_design", R.string.pesdk_textDesign_title_name, ImageSource.create(R.drawable.imgly_icon_tool_text_design)),
            ToolItem("imgly_tool_transform", R.string.pesdk_transform_title_name, ImageSource.create(R.drawable.imgly_icon_tool_transform)),
            ToolItem("imgly_tool_adjustment", R.string.pesdk_adjustments_title_name, ImageSource.create(R.drawable.imgly_icon_tool_adjust)),
            ToolItem("imgly_tool_sticker_selection", R.string.pesdk_sticker_title_name, ImageSource.create(R.drawable.imgly_icon_tool_sticker)),
            ToolItem("imgly_tool_text", R.string.pesdk_text_title_name, ImageSource.create(R.drawable.imgly_icon_tool_text)),
            ToolItem("imgly_tool_overlay", R.string.pesdk_overlay_title_name, ImageSource.create(R.drawable.imgly_icon_tool_overlay)),
            ToolItem("imgly_tool_frame", R.string.pesdk_frame_title_name, ImageSource.create(R.drawable.imgly_icon_tool_frame)),
            ToolItem("imgly_tool_brush", R.string.pesdk_brush_title_name, ImageSource.create(R.drawable.imgly_icon_tool_brush)),
            ToolItem("imgly_tool_focus", R.string.pesdk_focus_title_name, ImageSource.create(R.drawable.imgly_icon_tool_focus))
          )
        }
      }

      if (config.toHashMap()["limitCrop"] == true) {
        var limitCropWidth=((config.toHashMap()["limitCropWidth"] as? Double ?:0) as Double).roundToInt()
        var limitCropHeight=((config.toHashMap()["limitCropHeight"] as? Double ?:0) as Double).roundToInt()
        settingsList.getSettingsModel(AssetConfig::class.java).apply {
          getAssetMap(CropAspectAsset::class.java)
            .clear().add(
              CropAspectAsset("required_dimensions",
                limitCropWidth,
                limitCropHeight , false)
          )
        }


      }
    }

    //jgc end changes


    readSerialisation(settingsList, serialization, image == null)

    if (checkPermissions()) {
      startEditor(settingsList)
    }
  }

  private fun checkPermissions(): Boolean {
    (currentActivity as? PermissionAwareActivity)?.also {
      var haveAllPermissions = true
      for (permission in PermissionRequest.NEEDED_EDITOR_PERMISSIONS) {
        if (it.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
          haveAllPermissions = false
        }
      }
      if (!haveAllPermissions) {
        it.requestPermissions(PermissionRequest.NEEDED_EDITOR_PERMISSIONS, 0, this)
        return false
      }
    }

    return true
  }

  private fun readSerialisation(settingsList: SettingsList, serialization: String?, readImage: Boolean) {
    if (serialization != null) {
      skipIfNotExists {
        PESDKFileReader(settingsList).also {
          it.readJson(serialization, readImage)
        }
      }
    }
  }

  private fun startEditor(settingsList: PhotoEditorSettingsList?) {
    if (settingsList != null) {
      (currentActivity as? PermissionAwareActivity)?.also {
        for (permission in PermissionRequest.NEEDED_EDITOR_PERMISSIONS) {
          if (it.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            return
          }
        }
      }
      MainThreadRunnable {
        PhotoEditorBuilder(currentActivity)
          .setSettingsList(settingsList)
          .startActivityForResult(currentActivity, EDITOR_RESULT_ID)
      }()
    }
  }

  operator fun WritableMap.set(id: String, value: Boolean) = this.putBoolean(id, value)
  operator fun WritableMap.set(id: String, value: String?) = this.putString(id, value)
  operator fun WritableMap.set(id: String, value: Double) = this.putDouble(id, value)
  operator fun WritableMap.set(id: String, value: Float) = this.putDouble(id, value.toDouble())
  operator fun WritableMap.set(id: String, value: WritableArray?) = this.putArray(id, value)
  operator fun WritableMap.set(id: String, value: Int) = this.putInt(id, value)
  operator fun WritableMap.set(id: String, value: WritableMap?) = this.putMap(id, value)

  fun reactMap(vararg pairs: Pair<String, Any?>): WritableMap {
    val map = Arguments.createMap()

    for (pair in pairs) {
      val id = pair.first
      when (val value = pair.second) {
        is String? -> map[id] = value
        is Boolean -> map[id] = value
        is Double -> map[id] = value
        is Float -> map[id] = value
        is Int -> map[id] = value
        is WritableMap? -> map[id] = value
        is WritableArray? -> map[id] = value
        else -> if (value == null) {
          map.putNull(id)
        } else {
          throw RuntimeException("Type not supported by WritableMap")
        }
      }
    }

    return map
  }


  object ReactJSON {
    @Throws(JSONException::class) fun convertJsonToMap(jsonObject: JSONObject): WritableMap? {
      val map: WritableMap = WritableNativeMap()
      val iterator: Iterator<String> = jsonObject.keys()
      while (iterator.hasNext()) {
        val key = iterator.next()
        val value: Any = jsonObject.get(key)
        when (value) {
          is JSONObject -> {
            map.putMap(key, convertJsonToMap(value))
          }
          is JSONArray -> {
            map.putArray(key, convertJsonToArray(value))
          }
          is Boolean -> {
            map.putBoolean(key, value)
          }
          is Int -> {
            map.putInt(key, value)
          }
          is Double -> {
            map.putDouble(key, value)
          }
          is String -> {
            map.putString(key, value)
          }
          else -> {
            map.putString(key, value.toString())
          }
        }
      }
      return map
    }

    @Throws(JSONException::class) fun convertJsonToArray(jsonArray: JSONArray): WritableArray? {
      val array: WritableArray = WritableNativeArray()
      for (i in 0 until jsonArray.length()) {
        when (val value: Any = jsonArray.get(i)) {
          is JSONObject -> {
            array.pushMap(convertJsonToMap(value))
          }
          is JSONArray -> {
            array.pushArray(convertJsonToArray(value))
          }
          is Boolean -> {
            array.pushBoolean(value)
          }
          is Int -> {
            array.pushInt(value)
          }
          is Double -> {
            array.pushDouble(value)
          }
          is String -> {
            array.pushString(value)
          }
          else -> {
            array.pushString(value.toString())
          }
        }
      }
      return array
    }
  }

  override fun getName() = "RNPhotoEditorSDK"

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray): Boolean {
    PermissionRequest.onRequestPermissionsResult(requestCode, permissions, grantResults)
    startEditor(currentSettingsList)
    return false
  }

}
