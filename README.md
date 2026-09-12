# FreeShield VPN — Android Client

একটি আধুনিক, সুরক্ষিত ও স্বয়ংসম্পূর্ণ ওপেন-সোর্স অ্যান্ড্রয়েড VPN অ্যাপ যা নেটিভ অ্যান্ড্রয়েড `VpnService` ফ্রেমওয়ার্কের ওপর নির্মিত।

---

## 🚀 GitHub Actions দিয়ে যেভাবে সরাসরি APK তৈরি ও ডাউনলোড করবেন

এই রিপোজিটরিতে সম্পূর্ণ স্বয়ংক্রিয় **GitHub Actions CI/CD Workflow** যুক্ত করা হয়েছে। নিচে দেওয়া সহজ ধাপগুলো অনুসরণ করে আপনি গিটহাব থেকে সরাসরি APK ডাউনলোড করতে পারবেন:

### ধাপ ১: প্রজেক্টটি GitHub-এ Push করুন
1. AI Studio-এর সেটিংস/মেনু থেকে **"Push to GitHub"** বাটন ব্যবহার করে আপনার গিটহাব অ্যাকাউন্টে রিপোজিটরি তৈরি করুন, অথবা লোকাল গিট দিয়ে পুশ করুন:
   ```bash
   git init
   git add .
   git commit -m "Initial commit of FreeShield VPN"
   git branch -M main
   git remote add origin https://github.com/<your-username>/<your-repo-name>.git
   git push -u origin main
   ```

### ধাপ ২: GitHub Actions স্বয়ংক্রিয়ভাবে APK তৈরি করবে
- আপনি যখনই কোড পুশ করবেন (`git push`), গিটহাবের **Actions** ট্যাব স্বয়ংক্রিয়ভাবে কাজ শুরু করবে।
- এছাড়াও ম্যানুয়ালি বিল্ড করতে:
  1. আপনার রিপোজিটরির **Actions** ট্যাবে যান।
  2. বাঁদিকের তালিকা থেকে **"Build Android APK"** ওয়ার্কফ্লো নির্বাচন করুন।
  3. ডানপাশের **"Run workflow"** ড্রপডাউনে ক্লিক করে **Run workflow** বাটনে চাপুন।

### ধাপ ৩: তৈরি হওয়া APK ফাইলটি ডাউনলোড করুন
1. বিল্ড সম্পন্ন হলে (সবুজ টিক চিহ্ন আসবে) ওই বিল্ড রানের ওপর ক্লিক করুন।
2. পাতার নিচে **Artifacts** সেকশনে যান।
3. **`FreeShield-VPN-APKs`** নামের ফাইলটিতে ক্লিক করলেই আপনার ফোনে ইন্সটলযোগ্য `FreeShield-VPN-debug.apk` ডাউনলোড হয়ে যাবে!

---

## 🛠️ লোকাল কম্পিউটারে APK তৈরি করার নিয়ম

যদি আপনি আপনার নিজস্ব পিসি বা ল্যাপটপে APK বিল্ড করতে চান:

```bash
# লিনাক্স বা ম্যাকওএস:
chmod +x gradlew
./gradlew assembleDebug

# উইন্ডোজ:
gradlew.bat assembleDebug
```

বিল্ড সম্পন্ন হওয়ার পর APK পাওয়া যাবে এই পাথে:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔑 বিল্ড সিস্টেমের বৈশিষ্ট্য
- **স্বয়ংক্রিয় কি-স্টোর সেটআপ:** `debug.keystore.base64` থেকে স্বয়ংক্রিয়ভাবে কি-স্টোর রিস্টোর করে বিল্ড সফল নিশ্চিত করা হয়।
- **SDK ও JDK সামঞ্জস্যতা:** Java 21 ও Android SDK 36 সাপোর্ট সহ অপ্টিমাইজড ক্যাশিং।
- **ট্যাগ রিলিজ:** Git Tag (যেমন `v1.0.0`) পুশ করলে সরাসরি GitHub Releases সেকশনে APK ফাইল অ্যাটাচ হয়ে যাবে।
