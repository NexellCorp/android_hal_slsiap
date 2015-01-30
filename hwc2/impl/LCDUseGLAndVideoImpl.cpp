#undef LOG_TAG
#define LOG_TAG     "LCDUseGLAndVideoImpl"

#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>

#include <sys/ioctl.h>

#include <linux/fb.h>
#include <linux/media.h>
#include <linux/v4l2-subdev.h>
#include <linux/v4l2-mediabus.h>
#include <linux/videodev2.h>
#include <linux/ion.h>
#include <linux/nxp_ion.h>
#include <linux/videodev2_nxp_media.h>

#include <ion/ion.h>
#include <android-nxp-v4l2.h>
#include <nxp-v4l2.h>

#include <cutils/log.h>

#include <hardware/hwcomposer.h>
#include <hardware/hardware.h>

#include <gralloc_priv.h>

#include "HWCRenderer.h"
#include "LCDRGBRenderer.h"
#include "HWCCommonRenderer.h"

#include "HWCImpl.h"
#include "LCDCommonImpl.h"
#include "LCDUseGLAndVideoImpl.h"

using namespace android;

LCDUseGLAndVideoImpl::LCDUseGLAndVideoImpl(int rgbID, int videoID)
    :LCDCommonImpl(rgbID, videoID),
    mRGBRenderer(NULL),
    mRGBRenderer2(NULL),
    mVideoRenderer(NULL),
    mRGBHandle(NULL),
    mRGBHandle2(NULL),
    mVideoHandle(NULL),
    mRGBLayer2Configured(false),
    mOverlayConfigured(false),
    mRGBLayerIndex(-1),
    mRGBLayerIndex2(-1),
    mOverlayLayerIndex(-1)
{
    init();
}

LCDUseGLAndVideoImpl::LCDUseGLAndVideoImpl(int rgbID, int videoID, int width, int height)
    :LCDCommonImpl(rgbID, videoID, width, height),
    mRGBRenderer(NULL),
    mRGBRenderer2(NULL),
    mVideoRenderer(NULL),
    mRGBHandle(NULL),
    mRGBHandle2(NULL),
    mVideoHandle(NULL),
    mRGBLayer2Configured(false),
    mOverlayConfigured(false),
    mRGBLayerIndex(-1),
    mRGBLayerIndex2(-1),
    mOverlayLayerIndex(-1)
{
    init();
}

LCDUseGLAndVideoImpl::~LCDUseGLAndVideoImpl()
{
    if (mRGBRenderer)
        delete mRGBRenderer;
    if (mRGBRenderer2)
        delete mRGBRenderer2;
    if (mVideoRenderer)
        delete mVideoRenderer;
}

void LCDUseGLAndVideoImpl::init()
{
    ALOGV("%s", __func__);
    mRGBRenderer = new LCDRGBRenderer(mRgbID);
    if (!mRGBRenderer)
        ALOGE("FATAL: can't create RGBRenderer");

    mRGBRenderer2 = new HWCCommonRenderer(mRgbID, 4, 1);
    if (!mRGBRenderer2)
        ALOGE("FATAL: can't create RGBRenderer2");

    mVideoRenderer = new HWCCommonRenderer(mVideoID, 4);
    if (!mVideoRenderer)
        ALOGE("FATAL: can't create VideoRenderer");
}

int LCDUseGLAndVideoImpl::configOverlay(struct hwc_layer_1 &layer)
{
    int ret;

    ALOGD("configOverlay");

    ret = v4l2_set_format(mVideoID,
            layer.sourceCrop.right - layer.sourceCrop.left,
            layer.sourceCrop.bottom - layer.sourceCrop.top,
            // psw0523 fix for new gralloc
            //V4L2_PIX_FMT_YUV420M);
            V4L2_PIX_FMT_YUV420);
    if (ret < 0) {
        ALOGE("failed to v4l2_set_format()");
        return ret;
    }

    mLeft = layer.displayFrame.left;
    mTop  = layer.displayFrame.top;
    mRight = layer.displayFrame.right;
    mBottom = layer.displayFrame.bottom;

    ret = v4l2_set_crop(mVideoID, mLeft, mTop, mRight - mLeft, mBottom - mTop);
    if (ret < 0) {
        ALOGE("failed to v4l2_set_crop()");
        return ret;
    }

    ret = v4l2_reqbuf(mVideoID, 4);
    if (ret < 0) {
        ALOGE("failed to v4l2_reqbuf()");
        return ret;
    }

    //ret = v4l2_set_ctrl(mVideoID, V4L2_CID_MLC_VID_PRIORITY, 1);
    ret = v4l2_set_ctrl(mVideoID, V4L2_CID_MLC_VID_PRIORITY, 2);
    if (ret < 0) {
        ALOGE("failed to v4l2_set_ctrl()");
        return ret;
    }

    mOverlayConfigured = true;
    return 0;
}

int LCDUseGLAndVideoImpl::configRGBOverlay(struct hwc_layer_1 &layer)
{
    int ret;

    ALOGD("configRGBOverlay");

    ret = v4l2_set_format(mRgbID,
            layer.sourceCrop.right - layer.sourceCrop.left,
            layer.sourceCrop.bottom - layer.sourceCrop.top,
            V4L2_PIX_FMT_RGB32);
    if (ret < 0) {
        ALOGE("failed to v4l2_set_format()");
        return ret;
    }

    mRGBLeft = layer.displayFrame.left;
    mRGBTop  = layer.displayFrame.top;
    mRGBRight = layer.displayFrame.right;
    mRGBBottom = layer.displayFrame.bottom;

    ret = v4l2_set_crop(mRgbID, mRGBLeft, mRGBTop, mRGBRight - mRGBLeft, mRGBBottom - mRGBTop);
    if (ret < 0) {
        ALOGE("failed to v4l2_set_crop()");
        return ret;
    }

    ret = v4l2_reqbuf(mRgbID, 4);
    if (ret < 0) {
        ALOGE("failed to v4l2_reqbuf()");
        return ret;
    }

    mRGBLayer2Configured = true;
    return 0;
}

int LCDUseGLAndVideoImpl::configCrop(struct hwc_layer_1 &layer)
{
    if (mLeft != layer.displayFrame.left ||
        mTop != layer.displayFrame.top ||
        mRight != layer.displayFrame.right ||
        mBottom != layer.displayFrame.bottom) {
        mLeft = layer.displayFrame.left;
        mTop  = layer.displayFrame.top;
        mRight = layer.displayFrame.right;
        mBottom = layer.displayFrame.bottom;

        int ret = v4l2_set_crop(mVideoID, mLeft, mTop, mRight - mLeft, mBottom - mTop);
        if (ret < 0)
            ALOGE("failed to v4l2_set_crop()");

        return ret;
    } else {
        return 0;
    }
}

int LCDUseGLAndVideoImpl::configRGBCrop(struct hwc_layer_1 &layer)
{
    if (mRGBLeft != layer.displayFrame.left ||
        mRGBTop != layer.displayFrame.top ||
        mRGBRight != layer.displayFrame.right ||
        mRGBBottom != layer.displayFrame.bottom) {
        mRGBLeft = layer.displayFrame.left;
        mRGBTop  = layer.displayFrame.top;
        mRGBRight = layer.displayFrame.right;
        mRGBBottom = layer.displayFrame.bottom;

        int ret = v4l2_set_crop(mRgbID, mRGBLeft, mRGBTop, mRGBRight - mRGBLeft, mRGBBottom - mRGBTop);
        if (ret < 0)
            ALOGE("failed to v4l2_set_crop()");

        return ret;
    } else {
        return 0;
    }
}

int LCDUseGLAndVideoImpl::prepare(hwc_display_contents_1_t *contents)
{
    mRGBLayerIndex = -1;
    mRGBLayerIndex2 = -1;
    mOverlayLayerIndex = -1;

    // psw0523 test for miware
    //ALOGD("prepare: numHwLayers %d", contents->numHwLayers);
    //int numRGBLayers = contents->numHwLayers;

    for (size_t i = 0; i < contents->numHwLayers; i++) {
        hwc_layer_1_t &layer = contents->hwLayers[i];

        if (layer.compositionType == HWC_FRAMEBUFFER_TARGET) {
            mRGBLayerIndex = i;
            ALOGV("prepare: rgb %d", i);
            //numRGBLayers--;
            continue;
        }

        if (layer.compositionType == HWC_BACKGROUND)
            continue;

        if (mOverlayLayerIndex == -1 && canOverlay(layer)) {
            layer.compositionType = HWC_OVERLAY;
            layer.hints |= HWC_HINT_CLEAR_FB;
            mOverlayLayerIndex = i;
            ALOGV("prepare: overlay %d", i);
            //numRGBLayers--;
            continue;
        }

        layer.compositionType = HWC_FRAMEBUFFER;
    }

    //if (numRGBLayers == 1) {
    if (mOverlayLayerIndex >= 0) {
        for (size_t i = 0; i < contents->numHwLayers; i++) {
            hwc_layer_1_t &layer = contents->hwLayers[i];
            if (layer.compositionType == HWC_FRAMEBUFFER) {
                int width = layer.sourceCrop.right - layer.sourceCrop.left;
                int height = layer.sourceCrop.bottom - layer.sourceCrop.top;
                if (width == 1920 && height == 1080) {
                    layer.compositionType = HWC_OVERLAY;
                    ALOGV("Use RGB Layer!!");
                    mRGBLayerIndex = i;
                } else {
                    ALOGV("other layer %dx%d --> %dx%d", layer.sourceCrop.left, layer.sourceCrop.top, width, height);
                    mRGBLayerIndex2 = i;
                    layer.compositionType = HWC_OVERLAY;
                }
            }
        }
    }

    return 0;
}

int LCDUseGLAndVideoImpl::set(hwc_display_contents_1_t *contents, void *unused)
{
    mRGBHandle = NULL;
    mRGBHandle2 = NULL;
    mVideoHandle = NULL;

#if 0
    mOverlayLayerIndex = -1;
    for (size_t i = 0; i < contents->numHwLayers; i++) {
        hwc_layer_1_t &layer = contents->hwLayers[i];

        if (layer.compositionType == HWC_FRAMEBUFFER_TARGET)
            continue;

        if (layer.compositionType == HWC_BACKGROUND)
            continue;

        if (mOverlayLayerIndex == -1 && canOverlay(layer)) {
            mOverlayLayerIndex = i;
            break;
        }
    }
#endif

    ALOGV("set: rgb %d, overlay %d", mRGBLayerIndex, mOverlayLayerIndex);
    if (mOverlayLayerIndex >= 0) {
        mVideoHandle = reinterpret_cast<private_handle_t const *>(contents->hwLayers[mOverlayLayerIndex].handle);
        mVideoRenderer->setHandle(mVideoHandle);
        ALOGV("Set Video Handle: %p", mVideoHandle);
    }

    if (mRGBLayerIndex >= 0) {
        mRGBHandle = reinterpret_cast<private_handle_t const *>(contents->hwLayers[mRGBLayerIndex].handle);
        mRGBRenderer->setHandle(mRGBHandle);
        ALOGV("Set RGB Handle: %p", mRGBHandle);
    }

    if (mRGBLayerIndex2 >= 0) {
        mRGBHandle2 = reinterpret_cast<private_handle_t const *>(contents->hwLayers[mRGBLayerIndex2].handle);
        mRGBRenderer2->setHandle(mRGBHandle2);
        ALOGV("Set RGB2 Handle: %p, format %d", mRGBHandle2, mRGBHandle2->format);
    }

    if (!mOverlayConfigured && mVideoHandle) {
        configOverlay(contents->hwLayers[mOverlayLayerIndex]);
        mVideoOffCount = 0;
    } else if (mOverlayConfigured && !mVideoHandle) {
        mVideoOffCount++;
        if (mVideoOffCount > 1) {
            ALOGD("stop video layer");
            mVideoRenderer->stop();
            mOverlayConfigured = false;
        }
    } else if (mVideoHandle){
        mVideoOffCount = 0;
        configCrop(contents->hwLayers[mOverlayLayerIndex]);
    }

    if (!mRGBLayer2Configured && mRGBHandle2) {
        configRGBOverlay(contents->hwLayers[mRGBLayerIndex2]);
        mRGB2OffCount = 0;
    } else if (mRGBLayer2Configured && !mRGBHandle2) {
        mRGB2OffCount++;
        if (mRGB2OffCount > 1) {
            ALOGD("stop rgb2 layer");
            mRGBRenderer2->stop();
            mRGBLayer2Configured = false;
        }
    } else if (mRGBHandle2) {
        mRGB2OffCount = 0;
        configRGBCrop(contents->hwLayers[mRGBLayerIndex2]);
    }

    return 0;
}

private_handle_t const *LCDUseGLAndVideoImpl::getRgbHandle()
{
    return mRGBHandle;
}

private_handle_t const *LCDUseGLAndVideoImpl::getVideoHandle()
{
    return mVideoHandle;
}

int LCDUseGLAndVideoImpl::render()
{
    ALOGV("Render");
    if (mVideoHandle)
        mVideoRenderer->render();
    if (mRGBHandle)
        mRGBRenderer->render();
    if (mRGBHandle2)
        mRGBRenderer2->render();
    return 0;
}
