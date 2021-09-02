import json

startClusters = [
    '610957a8f784f130273f610d',
    '61096075d5e4fafbb4f44dca'
]

destinationClusters = [
     '610957a8f784f130273f610d',
     '61096075d5e4fafbb4f44dca'
]

data = {
    'startClusters' : startClusters,
    'destinationClusters' : destinationClusters
}

with open('src/main/resources/LocationClusteringResult.json', 'w') as outfile:
    json.dump(data, outfile)

print("Hello World from Python2")