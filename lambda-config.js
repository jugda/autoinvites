module.exports = {
  region: 'eu-west-1',
  handler: 'index.handler',
  role: '<role_arn>',
  functionName: 'sendInvites',
  description: 'Automatic email invites based on iCal entries',
  timeout: 5,
  memorySize: 128,
  runtime: 'nodejs'
}
