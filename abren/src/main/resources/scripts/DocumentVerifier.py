import json

data = {
    'isVerified' : True,
    'isFaceVerified': True,
    'isLogoVerified': True,
    'isTextVerified': True
}

idCardData = {
    'name' : 'Test Name',
    'dateOfBirth': 'Aug 27, 1997',
    'sex': 'F',
    'issueDate' : 'Oct 06, 2020',
    'expiryDate': 'Oct 02, 2020',
}

drivingLicenseData = {
    'issueDate' : 'Oct 06, 2020',
    'expiryDate': 'Oct 02, 2020',
}

data['idCardData'] = idCardData
data['drivingLicenseData'] = drivingLicenseData

with open('src/main/resources/DocumentVerifierResult.json', 'w') as outfile:
    json.dump(data, outfile)

print("Hello World from Python")