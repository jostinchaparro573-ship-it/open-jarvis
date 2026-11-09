# Jarvis — App identity, creator credits and third-party notices

Status: product requirement; final names and providers must be confirmed before release.

## In-app location

Add an **About Jarvis / Acerca de Jarvis** entry inside Settings. It should open a readable page with these sections:

1. **Product identity**
   - Product name: Jarvis (final branding subject to name/trademark review).
   - Edition: personal Android and Windows edition.
   - Version and build number.
   - Supported platforms: Android 12+ and Windows 11+ once verified.

2. **Creator and project ownership**
   - Creator / project lead: [creator display name — confirm with the user].
   - Project owner/contact: [to be confirmed].
   - Copyright notice for original code, UI, documentation, and other original contributions created for this Jarvis derivative: [year and rights holder — confirm before release].
   - Clearly distinguish original contributions in this derivative from inherited Open Jarvis source code and third-party components.

3. **Original project and open-source notices**
   - Upstream project: Open Jarvis by TokenArc.
   - Upstream repository: https://github.com/tokenarc/open-jarvis
   - Preserve the upstream MIT license and its copyright notice. Do not replace or erase inherited notices.
   - Include a Third-party licenses / Acknowledgements page listing dependencies and their licenses.

4. **Infrastructure and service providers**
   - Hosting / server provider: [name, or “No hosted service” if fully local].
   - Authentication provider: [name, or “Not configured”].
   - AI model / inference providers: list only providers actually enabled by the user.
   - Database / synchronization / backup provider: [name, or “Local-only”].
   - Speech-to-text and text-to-speech providers: [actual implementation].
   - Crash reporting / analytics: [name, or “Not used”].
   - Website / domain / support contact: [only if configured].
   - For each cloud service, explain briefly what data is sent, why, and where its privacy policy can be found.

5. **Privacy and legal information**
   - Links to Privacy Policy, Terms, Open-source licenses, and contact/support, once these documents exist.
   - Explain whether the app works offline and which features require internet.
   - Do not display a vendor as a sponsor, partner, host, or provider unless that relationship is real and configured.

## Attribution and rights rules

- The app may credit the user as the creator and project lead of this customized Jarvis edition and of their own original contributions.
- Do not claim that the user solely authored the original upstream Open Jarvis code.
- Preserve all inherited copyright and license notices. The upstream repository currently contains an MIT license naming TokenArc; verify the exact upstream license again before distributing any release.
- Maintain a software bill of materials / dependency-license inventory before distribution.
- A copyright notice is an attribution notice, not proof of registration or a determination of ownership. Any formal registration, trademark, or commercial rights question requires separate legal review.
- Keep this page factual and dynamically accurate. Never hard-code hosting, AI, storage, or analytics vendors before they are selected and integrated.

## Release checklist

- [ ] Confirm creator display name and copyright holder wording with the user.
- [ ] Confirm final product name and check for conflicting trademarks.
- [ ] Confirm hosting, AI, auth, storage, speech, analytics and support providers.
- [ ] Review data flows and privacy policy against actual implementation.
- [ ] Generate third-party license notices and software bill of materials.
- [ ] Add and test the About screen on Android and Windows.
