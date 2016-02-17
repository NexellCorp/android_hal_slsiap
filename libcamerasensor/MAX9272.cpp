#include <linux/videodev2.h>
#include <nxp-v4l2.h>
#include "MAX9272.h"

using namespace android;

enum {
    WB_INCANDESCENT = 0,
    WB_FLUORESCENT,
    WB_DAYLIGHT,
    WB_CLOUDY,
    WB_TUNGSTEN,
    WB_AUTO,
    WB_MAX
};

enum {
    COLORFX_NONE = 0,
    COLORFX_MONO,
    COLORFX_SEPIA,
    COLORFX_NEGATIVE,
    COLORFX_MAX
};

#define MIN_EXPOSURE     -3
#define MAX_EXPOSURE     3

#define ARRAY_SIZE(x) (sizeof(x)/sizeof(x[0]))


const int32_t ResolutionMAX9272[] = {
    640, 480,
};

// TOP/system/media/camera/include/system/camera_metadata_tags.h
static const uint8_t AvailableAfModesMAX9272[] = {
    ANDROID_CONTROL_AF_MODE_OFF,
    ANDROID_CONTROL_AF_MODE_AUTO,
    ANDROID_CONTROL_AF_MODE_MACRO,
    ANDROID_CONTROL_AF_MODE_CONTINUOUS_PICTURE,
    ANDROID_CONTROL_AF_MODE_CONTINUOUS_VIDEO
};

static const uint8_t AvailableAeModesMAX9272[] = {
    ANDROID_CONTROL_AE_MODE_OFF,
    ANDROID_CONTROL_AE_MODE_ON,
    ANDROID_CONTROL_AE_MODE_ON_AUTO_FLASH
};

static const uint8_t SceneModeOverridesMAX9272[] = {
    // ANDROID_CONTROL_SCENE_MODE_PORTRAIT
    ANDROID_CONTROL_AE_MODE_ON,
    ANDROID_CONTROL_AWB_MODE_AUTO,
    ANDROID_CONTROL_AF_MODE_CONTINUOUS_PICTURE,
    // ANDROID_CONTROL_SCENE_MODE_LANDSCAPE
    ANDROID_CONTROL_AE_MODE_ON,
    ANDROID_CONTROL_AWB_MODE_AUTO,
    ANDROID_CONTROL_AF_MODE_CONTINUOUS_PICTURE,
    // ANDROID_CONTROL_SCENE_MODE_SPORTS
    ANDROID_CONTROL_AE_MODE_ON,
    ANDROID_CONTROL_AWB_MODE_DAYLIGHT,
    ANDROID_CONTROL_AF_MODE_CONTINUOUS_PICTURE,
    // ANDROID_CONTROL_SCENE_MODE_NIGHT
#if 0
    ANDROID_CONTROL_AE_MODE_ON_AUTO_FLASH,
    ANDROID_CONTROL_AWB_MODE_AUTO,
    ANDROID_CONTROL_AF_MODE_CONTINUOUS_PICTURE
#endif
};

static const uint8_t AvailableEffectsMAX9272[] = {
    ANDROID_CONTROL_EFFECT_MODE_MONO,
    ANDROID_CONTROL_EFFECT_MODE_NEGATIVE,
    ANDROID_CONTROL_EFFECT_MODE_SEPIA,
    ANDROID_CONTROL_EFFECT_MODE_AQUA
};

static const uint8_t AvailableSceneModesMAX9272[] = {
    ANDROID_CONTROL_SCENE_MODE_PORTRAIT,
    ANDROID_CONTROL_SCENE_MODE_LANDSCAPE,
    ANDROID_CONTROL_SCENE_MODE_SPORTS,
#if 0
    ANDROID_CONTROL_SCENE_MODE_NIGHT
#endif
};

static const int32_t AvailableFpsRangesMAX9272[] = {
    15, 30
};

static const uint32_t ExposureCompensationRangeMAX9272[] = {
     MIN_EXPOSURE, MAX_EXPOSURE
};

static const uint8_t AvailableAntibandingModesMAX9272[] = {
    ANDROID_CONTROL_AE_ANTIBANDING_MODE_OFF,
    ANDROID_CONTROL_AE_ANTIBANDING_MODE_50HZ,
    ANDROID_CONTROL_AE_ANTIBANDING_MODE_60HZ
};

static const uint8_t AvailableAwbModesMAX9272[] = {
    ANDROID_CONTROL_AWB_MODE_OFF,
    ANDROID_CONTROL_AWB_MODE_AUTO,
    ANDROID_CONTROL_AWB_MODE_DAYLIGHT,
    ANDROID_CONTROL_AWB_MODE_CLOUDY_DAYLIGHT,
    ANDROID_CONTROL_AWB_MODE_FLUORESCENT,
    ANDROID_CONTROL_AWB_MODE_INCANDESCENT
};

void MAX9272::init()
{
    // TODO
    Width = ResolutionMAX9272[0];
    Height = ResolutionMAX9272[1];
    NumResolutions = ARRAY_SIZE(ResolutionMAX9272)/2;
    Resolutions = ResolutionMAX9272;
    NumAvailableAfModes = ARRAY_SIZE(AvailableAfModesMAX9272);
    AvailableAfModes = AvailableAfModesMAX9272;
    NumAvailableAeModes = ARRAY_SIZE(AvailableAeModesMAX9272);
    AvailableAeModes = AvailableAeModesMAX9272;
    NumSceneModeOverrides = ARRAY_SIZE(SceneModeOverridesMAX9272);
    SceneModeOverrides = SceneModeOverridesMAX9272;
    ExposureCompensationRange = ExposureCompensationRangeMAX9272;
    AvailableAntibandingModes = AvailableAntibandingModesMAX9272;
    NumAvailAntibandingModes = ARRAY_SIZE(AvailableAntibandingModesMAX9272);
    AvailableAwbModes = AvailableAwbModesMAX9272;
    NumAvailAwbModes = ARRAY_SIZE(AvailableAwbModesMAX9272);
    AvailableEffects = AvailableEffectsMAX9272;
    NumAvailEffects = ARRAY_SIZE(AvailableEffectsMAX9272);
    AvailableSceneModes = AvailableSceneModesMAX9272;
    NumAvailSceneModes = ARRAY_SIZE(AvailableSceneModesMAX9272);
    AvailableFpsRanges = AvailableFpsRangesMAX9272;
    NumAvailableFpsRanges = ARRAY_SIZE(AvailableFpsRangesMAX9272);

    // TODO
    FocalLength = 3.43f;
    Aperture = 2.7f;
    MinFocusDistance = 0.1f;
    FNumber = 2.7f;

    // TODO
    //MaxFaceCount = 16;
    MaxFaceCount = 1;

    // Crop
    CropWidth = Width;
    CropHeight = Height;
    CropLeft = 0;
    CropTop = 0;
}

MAX9272::MAX9272()
{
    init();
}

MAX9272::MAX9272(uint32_t v4l2ID)
    : NXCameraBoardSensor(v4l2ID)
{
    init();
}

MAX9272::~MAX9272()
{
}

void MAX9272::setAfMode(uint8_t afMode)
{
    AfMode = afMode;
}

void MAX9272::afEnable(bool enable)
{
}

void MAX9272::setEffectMode(uint8_t effectMode)
{
    if (effectMode != EffectMode) {
        uint32_t val = 0;

        switch (effectMode) {
        case ANDROID_CONTROL_EFFECT_MODE_OFF:
            val = COLORFX_NONE;
            break;
        case ANDROID_CONTROL_EFFECT_MODE_MONO:
            val = COLORFX_MONO;
            break;
        case ANDROID_CONTROL_EFFECT_MODE_NEGATIVE:
            val = COLORFX_NEGATIVE;
            break;
        case ANDROID_CONTROL_EFFECT_MODE_SEPIA:
            val = COLORFX_SEPIA;
            break;
        default:
            //ALOGE("%s: unsupported effectmode 0x%x", __func__, effectMode);
            return;
        }

        v4l2_set_ctrl(V4l2ID, V4L2_CID_COLORFX, val);
        EffectMode = effectMode;
    }
}

void MAX9272::setSceneMode(uint8_t sceneMode)
{
}

void MAX9272::setAntibandingMode(uint8_t antibandingMode)
{
}

void MAX9272::setAwbMode(uint8_t awbMode)
{
    if (awbMode != AwbMode) {
        uint32_t val = 0;

        switch (awbMode) {
        case ANDROID_CONTROL_AWB_MODE_OFF:
            v4l2_set_ctrl(V4l2ID, V4L2_CID_AUTO_WHITE_BALANCE, 0);
            return;
        case ANDROID_CONTROL_AWB_MODE_AUTO:
            v4l2_set_ctrl(V4l2ID, V4L2_CID_AUTO_WHITE_BALANCE, 1);
            return;
        case ANDROID_CONTROL_AWB_MODE_DAYLIGHT:
            val = WB_DAYLIGHT;
            break;
        case ANDROID_CONTROL_AWB_MODE_CLOUDY_DAYLIGHT:
            val = WB_DAYLIGHT;
            break;
        case ANDROID_CONTROL_AWB_MODE_FLUORESCENT:
            val = WB_FLUORESCENT;
            break;
        case ANDROID_CONTROL_AWB_MODE_INCANDESCENT:
            val = WB_INCANDESCENT;
            break;
        default:
            //ALOGE("%s: unsupported awb mode 0x%x", __func__, awbMode);
            return;
        }
        AwbMode = awbMode;

        v4l2_set_ctrl(V4l2ID, V4L2_CID_WHITE_BALANCE_TEMPERATURE, val);
    }
}

void MAX9272::setExposure(int32_t exposure)
{
    if (exposure < MIN_EXPOSURE || exposure > MAX_EXPOSURE) {
        ALOGE("%s: invalid exposure %d", __func__, exposure);
        return;
    }

    if (exposure != Exposure) {
        Exposure = exposure;
        v4l2_set_ctrl(V4l2ID, V4L2_CID_BRIGHTNESS, exposure + 3);
    }
}

uint32_t MAX9272::getZoomFactor(void)
{
    // disable zoom
    return 1;
}

status_t MAX9272::setZoomCrop(uint32_t left, uint32_t top, uint32_t width, uint32_t height)
{
    return NO_ERROR;
}

int MAX9272::setFormat(int width, int height, int format)
{
    //ALOGD("[ %s ]: width : %d, height : %d, format : %d\n", __func__, width, height, format);

    int sensorWidth, sensorHeight;
#ifdef S5P4418
    sensorWidth = width + 32;
    sensorHeight = height + 1;
#else
    sensorWidth = width;
    sensorHeight = height;
#endif

    //ALOGD("[ %s ]: V4l2ID : %d\n", __func__, V4l2ID);

    return v4l2_set_format(V4l2ID, sensorWidth, sensorHeight, format);
}

bool MAX9272::isInterlace()
{
	return false;
}
