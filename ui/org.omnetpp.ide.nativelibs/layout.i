%module LayoutEngine

// covariant return type warning disabled
#pragma SWIG nowarn=822

%include "loadlib.i"
%include "enumtypeunsafe.swg"
%include "defs.i"
%javaconst(1);

%{
#include "layout/graphlayouter.h"
#include "layout/basicspringembedderlayout.h"
#include "layout/forcedirectedgraphlayouter.h"
// #include "layout/geometry.h"
// #include "layout/forcedirectedparametersbase.h"
// #include "layout/forcedirectedparameters.h"
// #include "layout/forcedirectedembedding.h"

using namespace omnetpp::layout;
%}

// hide export/import macros from swig
#define COMMON_API
#define LAYOUT_API
#define OPP_DLLEXPORT
#define OPP_DLLIMPORT

// %typemap(jni)    cModule* "jobject"
// %typemap(jtype)  cModule* "Object"
// %typemap(jstype) cModule* "Object"
// %typemap(javain) cModule* "$javainput"
//
//%include "std_common.i"
//%include "std_string.i"
//%include "std_vector.i"
//
// %typemap(javacode) Variable %{
//     protected Variable disown() {
//         swigCMemOwn = false;
//         return this;
//     }
//
//     public boolean equals(Object obj) {
//         return (obj instanceof Variable) && getCPtr(this)==getCPtr((Variable)obj);
//     }
//     public int hashCode() {
//         return (int)getCPtr(this);
//     }
// %}
//
// %typemap(javain) Variable * "Variable.getCPtr($javainput.disown())";
//
// %typemap(javain) IBody * "IBody.getCPtr($javainput.disown())";
//
// %typemap(javaimports) IBody %{
// import java.lang.reflect.Constructor;
// %}
//
// %typemap(javaout) IBody * {
//    return IBody.newIBody($jnicall, $owner);
// }
//
// %typemap(javacode) IBody %{
//    protected IBody disown() {
//       swigCMemOwn = false;
//       return this;
//    }
//
//    @SuppressWarnings("unchecked")
//    private static java.util.ArrayList<Constructor> bodyConstructors = new java.util.ArrayList<Constructor>();
//
//    @SuppressWarnings("unchecked")
//    public static IBody newIBody(long cPtr, boolean isOwner) {
//       try {
//          if (cPtr == 0)
//             return null;
//
//          String className = LayoutEngineJNI.IBody_getClassName(cPtr, null);
//          Constructor constructor = null;
//          for (int i = 0; i < bodyConstructors.size(); i++)
//             if (bodyConstructors.get(i).getName().equals(className))
//                constructor = bodyConstructors.get(i);
//
//          if (constructor == null)
//          {
//             String name = "org.omnetpp.common.engine." + className;
//             Class clazz = Class.forName(name);
//             constructor = clazz.getDeclaredConstructor(long.class, boolean.class);
//             bodyConstructors.add(constructor);
//          }
//
//          return (IBody)constructor.newInstance(cPtr, isOwner);
//       }
//       catch (Exception e) {
//          throw new RuntimeException(e);
//       }
//    }
// %}
//
// %typemap(javain) IForceProvider * "IForceProvider.getCPtr($javainput.disown())";
//
// %typemap(javaimports) IForceProvider %{
// import java.lang.reflect.Constructor;
// %}
//
// %typemap(javaout) IForceProvider * {
//    return IForceProvider.newIForceProvider($jnicall, $owner);
// }
//
// %typemap(javacode) IForceProvider %{
//    protected IForceProvider disown() {
//       swigCMemOwn = false;
//       return this;
//    }
//
//    @SuppressWarnings("unchecked")
//    private static java.util.ArrayList<Constructor> forceProviderConstructors = new java.util.ArrayList<Constructor>();
//
//    @SuppressWarnings("unchecked")
//    public static IForceProvider newIForceProvider(long cPtr, boolean isOwner) {
//       try {
//          if (cPtr == 0)
//             return null;
//
//          // we implement polymorphic return types:
//          String className = LayoutEngineJNI.IForceProvider_getClassName(cPtr, null);
//          Constructor constructor = null;
//          for (int i = 0; i < forceProviderConstructors.size(); i++)
//             if (forceProviderConstructors.get(i).getName().equals(className))
//                constructor = forceProviderConstructors.get(i);
//
//          if (constructor == null)
//          {
//             String name = "org.omnetpp.common.engine." + className;
//             Class clazz = Class.forName(name);
//             constructor = clazz.getDeclaredConstructor(long.class, boolean.class);
//             forceProviderConstructors.add(constructor);
//          }
//
//          return (IForceProvider)constructor.newInstance(cPtr, isOwner);
//       }
//       catch (Exception e) {
//          throw new RuntimeException(e);
//       }
//    }
// %}
//
// namespace std {
//    %template(CcList) vector<Cc>;
//    %template(IBodyList) vector<IBody *>;
//    %template(IForceProviderList) vector<IForceProvider *>;
//    %template(PtList) vector<Pt>;
//    %template(VariableList) vector<Variable *>;
// };

%define FIXUP_GETNODEPOSITION(CLASS)
%ignore CLASS::getNodePosition;
%extend CLASS {
   double getNodePositionX(int mod) {double x,y; self->getNodePosition(mod, x, y); return x;}
   double getNodePositionY(int mod) {double x,y; self->getNodePosition(mod, x, y); return y;}
}
%enddef

namespace omnetpp { namespace layout {

FIXUP_GETNODEPOSITION(GraphLayouter);
FIXUP_GETNODEPOSITION(BasicSpringEmbedderLayout);
FIXUP_GETNODEPOSITION(ForceDirectedGraphLayouter);

} } // namespaces

///*
//XXX to ignore:
//  getDistanceAndVector()
//  getStandardVerticalDistanceAndVector()
//  getSlipperyDistanceAndVector()
//*/
//
//%ignore zero;
//%ignore NaN;
//%ignore POSITIVE_INFINITY;
//%ignore isNaN;
//%ignore ForceDirectedEmbedding::parameters;
//%ignore ForceDirectedEmbedding::writeDebugInformation;
//%ignore LeastExpandedSpring::LeastExpandedSpring;   //XXX takes std::vector<AbstractSpring*>

%include "layout/graphlayouter.h"
%include "layout/basicspringembedderlayout.h"
%include "layout/forcedirectedgraphlayouter.h"
// %include "layout/geometry.h"
// %include "layout/forcedirectedparametersbase.h"
// %include "layout/forcedirectedparameters.h"
// %include "layout/forcedirectedembedding.h"

