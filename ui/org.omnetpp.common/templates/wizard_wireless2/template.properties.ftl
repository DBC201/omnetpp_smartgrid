templateName = Newly Generated Dummy Wireless Network Wizard #2
templateDescription = An example wizard which can generate a project, simulation, nedfile or network
templateCategory = Newly Generated Wizards
supportedWizardTypes = project, simulation, nedfile, network

# default values
numNodes = 22
placement = grid
routingProtocol = AODV
gridColumns = 8
gridHSpacing = 20
gridVSpacing = 20

# custom pages
page.1.file = general.xswt
page.1.title = General

page.2.file = routing.xswt
page.2.title = Choose Routing Protocol
page.2.condition = wantRouting

page.3.file = aodvOptions.xswt
page.3.title = AODV Options
page.3.condition = wantRouting && routingProtocol=="AODV"

page.4.file = dsdvOptions.xswt
page.4.title = DSDV Options
page.4.condition = wantRouting && routingProtocol=="DSDV"

page.5.file = gridPlacement.xswt
page.5.condition = placement == "grid"
