<@setoutput path=srcFolder+"/Sink.h"/>
${bannerComment}

#ifndef __${PROJECTNAME}_SINK_H
#define __${PROJECTNAME}_SINK_H

#include <omnetpp.h>

using namespace omnetpp;

<#if namespace!="">namespace ${namespace} {</#if>

/**
 * Message sink; see NED file for more info.
 */
class Sink : public cSimpleModule
{
  private:
    // state
    simtime_t lastArrival;

    // statistics
    cHistogram iaTimeHistogram;
    cOutVector arrivalsVector;

  protected:
    virtual void initialize();
    virtual void handleMessage(cMessage *msg);
    virtual void finish();
};

<#if namespace!="">}; // namespace</#if>

#endif


