#ifndef _DUMMYIMPL_H
#define _DUMMYIMPL_H

namespace android {

class DummyImpl: public HDMICommonImpl
{
public:
    DummyImpl() {
    }
    virtual ~DummyImpl() {
    }

    virtual int enable() {
        return 0;
    }
    virtual int disable() {
        return 0;
    }
    virtual int prepare(hwc_display_contents_1_t *content) {
        return 0;
    }
    virtual int set(hwc_display_contents_1_t *content, void *reserve) {
        return 0;
    }
    virtual private_handle_t const *getRgbHandle() {
        return NULL;
    }
    virtual private_handle_t const *getVideoHandle() {
        return NULL;
    }
    virtual int render() {
        return 0;
    }
};

}; // namespace
#endif
