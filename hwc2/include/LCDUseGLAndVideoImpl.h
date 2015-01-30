#ifndef _LCDUSEGLANDVIDEOIMPL_H
#define _LCDUSEGLANDVIDEOIMPL_H

class LCDCommonImpl;
struct hwc_layer_1;

namespace android {

class LCDUseGLAndVideoImpl: public LCDCommonImpl
{
public:
    LCDUseGLAndVideoImpl(int rgbID, int videoID);
    LCDUseGLAndVideoImpl(int rgbID, int videoID, int width, int height);
    virtual ~LCDUseGLAndVideoImpl();

    virtual int prepare(hwc_display_contents_1_t *);
    virtual int set(hwc_display_contents_1_t *, void *);
    virtual private_handle_t const *getRgbHandle();
    virtual private_handle_t const *getVideoHandle();
    virtual int render();

protected:
    virtual void init();

private:
    HWCRenderer *mRGBRenderer;
    HWCRenderer *mRGBRenderer2;
    HWCRenderer *mVideoRenderer;
    private_handle_t const *mRGBHandle;
    private_handle_t const *mRGBHandle2;
    private_handle_t const *mVideoHandle;
    bool mRGBLayer2Configured;
    bool mOverlayConfigured;
    int mRGBLayerIndex;
    int mRGBLayerIndex2;
    int mOverlayLayerIndex;

    // crop
    int mLeft;
    int mTop;
    int mRight;
    int mBottom;

    // RGB Crop
    int mRGBLeft;
    int mRGBTop;
    int mRGBRight;
    int mRGBBottom;

    int mVideoOffCount;
    int mRGB2OffCount;

private:
    int configOverlay(struct hwc_layer_1 &layer);
    int configCrop(struct hwc_layer_1 &layer);

    int configRGBOverlay(struct hwc_layer_1 &layer);
    int configRGBCrop(struct hwc_layer_1 &layer);
};

}; // namespace
#endif
