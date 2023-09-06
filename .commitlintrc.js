module.exports = {
    extends: ['@commitlint/config-conventional'],
    helpUrl: 'https://agogos.pages.redhat.com/agogos/docs/developer-guide/policy-guides/commit_message_format.html',
    rules: {
        'scope-empty': [2, 'never']
    },
    ignores: [
        (message) => message.startsWith('draft'),
        (message) => message.startsWith('Draft')
    ]
};
