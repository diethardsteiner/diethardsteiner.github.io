---
layout: post
title: "PDI Password Encryption"
summary: This article provides a short intro into PDI password encryption
date: 2017-03-03
categories: PDI
tags: PDI
published: true
--- 


## Encrypting a password in PDI

For **database connections** as well as various other sensistive accounts **encrypting** the **password** instead of storing it in plain-text within e.g. properties files is advisable. **PDI** ships with a handy **encryption tool** which is very easy to use:

```bash
$ ./encr.sh -kettle <your-password>
```
This command will return the encrypted password.

You get the general usage info by just typing:

```bash
$ ./encr.sh
encr usage:

  encr <-kettle|-carte> <password>
  Options:
    -kettle: generate an obfuscated password to include in Kettle XML files
    -carte : generate an obfuscated password to include in the carte password file 'pwd/kettle.pwd'
```

Example: Encrypting password `test`:

```bash
$ ./encr.sh -kettle test
Java HotSpot(TM) 64-Bit Server VM warning: ignoring option MaxPermSize=256m; support was removed in 8.0
19:53:07,542 INFO  [KarafBoot] Checking to see if org.pentaho.clean.karaf.cache is enabled
19:53:07,663 INFO  [KarafInstance] 
*******************************************************************************
*** Karaf Instance Number: 1 at /Applications/Development/pdi-ce-7.0/./syst ***
***   em/karaf/caches/encr/data-1                                           ***
*** Karaf Port:8802                                                         ***
*** OSGI Service Port:9051                                                  ***
*******************************************************************************
Mar 02, 2017 7:53:08 PM org.apache.karaf.main.Main$KarafLockCallback lockAquired
INFO: Lock acquired. Setting startlevel to 100
Encrypted 2be98afc86aa7f2e4cb79ce10ca97bcce
```

The encrypted password in this case is:

```
Encrypted 2be98afc86aa7f2e4cb79ce10ca97bcce
```

**Note**: Make sure you include `Encrypted ` when specifying the password!

## How to decrypt the password for steps that don't support it natively

Within PDI **Database connections** decrypt passwords natively, however, steps, like e.g. the **HTTP** step, don't dycrypt an encrypted password automatically. The work-around for this is to use a **UDJE** (User Defined Java Expression). 

Create a new field within the UDJE step called `decrypted_password` and add this Java snippet:

```java
import org.pentaho.di.core.encryption.Encr;Encr.decryptPassword(encrypted_password)
```

`encrypted_password` in the code snipped above is an incoming field containing ... well you guessed it.

How could you have found out about this function? 

Just search the **Pentaho Kettle Github repo**:

```
https://github.com/pentaho/pentaho-kettle/search?utf8=✓&q=decryptPassword&type=Code
```

The idea is to create a dedicate transformation which uses a **Get Variable** step to get hold of the encrypted password variable, then you can use the **User Defined Java Expression** to decrypt the password and finally use a **JavaScript** step to set the variable:

```javascript
setVariable("VAR_PW_DECRYPTED",decrypted_password,"r");
```

> **Important**: Do not use the **Set Variable** step as it will print out the decrypted password to the log.

