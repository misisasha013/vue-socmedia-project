# Project Charter (Template)

**Date Last Revised:** 4/25/26

**Team:** team-04

- <member name, degree, email, major>
- Sakshi Mishra, Computer Science, sam227
- Leo Xu, Computer Science, lex227
- Jack Lamb, Computer Science and Business, jal228
- Julia Salinero, Computer Science and graphic design, jus628
---

## OVERVIEW OF THIS DOCUMENT

This document is a project charter template, designed to help you consider the larger contexts and activities in which software engineering efforts take place. It should be completed incrementally and iteratively.

- Bullet points are often acceptable.
- If you spend more than 1 hour on this document during any increment, you are likely spending too much time.
- This document focuses on business requirements.

---

## Why is this important?

A project charter attempts to concisely capture the relevant details of the business problem you are solving. It is written based on the current level of knowledge and will evolve as needed.

### Key Questions to Answer:
1. Why are you building this? What are you trying to do?
2. How is it done today, and what are the limits of current practice?
3. Who cares? If successful, what difference will it make?
4. What are you going to build?
5. How will you build it?
6. How will we know that it works?
7. When will you build it? How long will it take?
8. What are the risks?
9. How much will it cost?
10. What will you do when it is done?

---

## 1. Executive Summary 

Summarize what is covered in the rest of the document (max 1-2 paragraphs; bullet points are acceptable).

- Description of problem and solution
- Schedule and feature description
- Summary of critical/significant things readers must know (scope, sponsor requirements, risks)

- Problem: Need to create a platform for company employees to document information about themselves, share ideas, files and links, and like/reply to those ideas. 
- Solution: Creating a paas application that allows for users to post ideas with links/files, replies to those ideas, like those ideas, and customize their profile
- Schedule: This is split up into 5 phases with 1-2 sprints contained within each phase (around 9 sprints total)
- Scope: User account authentication and profiles, Idea and reply posting system, liking and unliking ideas, 
- Sponsor requirements: Usability (intuitive UI), security (google oauth), scalability (able to handle using files/links), Maintainability (documentation), 
- Risks: Handling of sensitive information to google oauth, handling session information to memcachier, file storage concerns with google drive storage, 
---

## 2. Business Background (need to finish)

Describe the business, business strategy, and the business problem being solved.

### Key Components of a Business Problem Statement:
- **Problem:** Why is this challenge important for the business?
- **Impact:** Negative effects of the problem on the business
- **Desired Outcome:** The goal the customer wishes to achieve

---

### a. Resources Preassigned
List important time, personnel, and computing systems.
- Team members (frontend, backend, admin, PM)
- PostgreSQL database hosted on Dokku
- Development tools such as Java, Maven, Vue, and Jira

### b. Stakeholders
List important people or teams, their roles, and their involvement.
- Jack Lamb (jal228@lehigh.edu)
- Sakshi Mishra (sam227@lehigh.edu)
- Leo Xu (lex227@lehigh.edu)
- Julia Salinero (jus628@lehigh.edu)
### c. Stakeholder Requirements
Capture the most high-level/critical desires of the identified stakeholders.
- Secure user authentication using OAuth
- Ability to create, view, and interact with posts 
- Support for replies and likes on posts
- Ability to upload and access files and links
- Reliable and consistent database storage
### d. Assumptions
List likely conditions presumed known and true.
- Users have access to the internet and browsers
- Users will authenticate using Google OAuth
- The database (PostgreSQL) will remain available and reliable
- Team members have access to required development tools
### e. Constraints
List limiting conditions, circumstances, or events.
- Must use required technologies (Java, PostgreSQL, Vue, Dokku)
- Deployment must work within Dokku environment
### f. Measurable Project Objectives
Use SMART criteria: Specific, Measurable, Achievable, Relevant, Time-bound.
- Successfully deploy the application on Dokku by the end of Phase 5
- Implement all core features (posts, replies, likes, files, links)
- Ensure all unit tests pass successfully
---

## 3. Current Solution (need to answer)

Describe the current solution, whether automated or not.

- Right now, there is no single system for employees to share ideas and information. People mostly use tools like email or chat, which are not organized and make it easy to lose information.

- There is no easy way to store ideas, reply to them, or share files and links in one place, which makes collaboration harder.

- We created a fullstack application for all users within the company to be able to share ideas quickly, centrally, and share important files/links with each other as well. With a like system, these ideas and replies can be highlighted and with the profile feature, users in the company can see each other's public information. The company can also keep track of sensitive information with the profile feature without having to ask individually, which avoids pressure. 

---

## 4. Proposed Solution 

### a. Context Diagram
A diagram showing the scope of the project and the external stakeholders.
[Here is where you can find the system context diagram](docs\project_charter\systemcontextdiagram.png)
- it is systemcontextdiagram.png within this folder

### b. Technical Architecture
A description of what you will provide to solve the problem, including deployment strategy.
[Here is where you can find the technical architecture/deployment diagram](docs\project_charter\deploymentdiagram.png)
- it is deploymentdiagram.png within this folder

### c. Software Design Description (SDD)
List major software components and what each one will do.
- client- a fullstack app that allows users to post ideas and replies to those ideas, post files and links as ideas/replies, like those replies/ideas, log in with google oauth, and update personal and private information in their profiles. 
- backend- connects to external software like memchachier to store session data, googleoauth for authentication, external google drive to save files, and the postgres database. Handles cookie and session information 
- frontend- Uses components to create the user interface, sends user input to the backend and displays ideas, replies, likes to ideas/replies, profile information (public for others, public and personal for the user), and files/links within the ideas/replies. 
- Postgres database- Stores public and private information about authenticated users, posts and replies, likes on posts and replies, links, and file information 
- Dokku- hosts the app and connects to the postgres database 
- Google Oauth- store authentication information 
- Memcachier- stores state and session information 
- Google external drive- stores files 
- command line admin app- allows admins to make edits to the information stored in the database and add/remove validation on the things stored within the table 

### d. Development Architecture
List tools, languages, frameworks, etc.
- Javascript
- Typescript
- Java 
- PostgresSQL
- Vue and Vite
- google oauth 
- external google drive 
- memchachier

### e. Exclusions and Limitations
Indicate what the solution will not address.
- No mobile development on mobile browsers or as an application 
- no complex profile system with specific roles yet  
- not able to choose/set profile pictures

### f. Business Value
Roll up the value gained by key users and stakeholders.
- improved collaboration between employees, able to post important files/links and share ideas easily
- streamlined employee information system with employee profiles
- centralized platform for discussion 

---

## 5. Deliverables 

List what you must deliver to satisfy the course and project requirements.

- Deliverables:
- Secure, durably executing, hosted application with dokku
    - unit tests and manual testing done for components
- source and executable code merged to main branch of one repository
    - consistently updated code from sprint to sprint meeting each phase requirement, components updated in individual branches and then combined and tested in pre-main before pushing to main
- Postgres database with fully populated tables and schema
    - Admin command line app section of the project instantiates and provides ways to interact with the database, which is hosted separately on postgres and connected to the dokku app.
- accounts and passwords stored with oauth, files stored in external drive 
    - Backend uses the google oauth system upon login, and the frontend and backend both work to store files in an external drive 
- documentation for all parts of the program (system architecture, er diagrams, ui mockups)
    - Documentation hosted in a special folder in the repository and updated with each phase 

- Maintenance plan:
- The Dokku app is monitored for performance regularly and logs will be reviewed
- All bugs will be tracked within specific github version control and can be fixed within individual branches which are tested and then merged
- All dependencies will be monitored for major changes (especially google oauth) to make sure that app compatibility still works
- The postgres database will be monitored and updated regularly with new information and tested 
- Documentation will be updated whenever new features are added
- ENV variables will be securely maintaned locally and in private documents shared among team members to avoid security leaks 

---

## 6. Methodology and Schedule 

### a. Methodology
Briefly describe your use of Scrum.

- We Use scrum in our weekly Monday and Tuesday meetings for quick check ins and reviewing what everyone has accomplished during their week, and the objectives for next week. By doing it once a week, we get a good idea of where our group is at and we have time to work together for the rest of the meeting to make up for pitfalls or to do code reviews. 

### b. Schedule
Scrum-based sprint schedule with high-level target objectives.
- We use a jira board that follows the work breakdown structure. From the specific phase requirement page given every week/ two weeks, the PM breaks down the tasks given into smaller work objects and assigns them to the specific role in the group. The jira board is divided into specific sprints, and whatever is not completed in the initial sprint is placed in the backlog of the next sprint. Sprints are categorized additionally into major phases, which decide the specific deliverables for that phase. Every phase is 1-2 weeks, for two week phases the sprint workload is usually broken down into one week for planning and documentation, and one week for execution. In addition to phase requirements, tech debt is also added to the sprint backlog.

---

## 7. Risk Management 

### a. Testing
Document your tools and methodology for testing.

- Procedure for Manual testing: Code is reviewed manually by the PM during pull requests and in person recitation meetings, and the code is additionally reviewed during peer programming sessions where frontend and backend works together to connect their routes, or backend and admin works together to finalize data base methods and structure. Code reviews involve running the program and testing out some of the manipulatable methods, and resolving merge conflicts. 
- Procedure for Automated testing: Most unit testing is automated, especially for admin and backend testing. The automated testing procedure for admin revolves around creating unit tests for all the adding/dropping of tables, and the various table manipulating methods that would need to be manipulated. Every time a table gets created, unit testing gets updated for the admin. The backend is structured similarly with unit testing.

### b. Security
Consider how information is protected against malicious intent.

- Login and passwords are handled through google Ouath to prevent security risks and are not stored anywhere. Sensitive information regarding gender and sexuality are set through drop down menus rather than fill ins to prevent sql injection attacks, and are not visible to others. This information is stored in the database. Files are stored through an external google cloud service and only rudimentary information about them is stored within the database. Links are stored within the database. Client secret and general information like that is stored within a google document shared within the team rather than pushed to github or posted on slack. Admins are able to access the command line app and remove wrong items, but are not given access to the oauth secrets or the external drive. Developers have access to the admin command line app, database, oauth, and external drive. Users are not able to access or see other user's sensitive information, and are not able to remove ideas/replies/links/files that are not their own.

### c. Other Risks
List other risks and indicate mitigation strategies.
- We have validation columns listed on all of our tables to track validation on people, ideas, replies, and files/links. An admin is able to remove the valid status on any of these which prevents them from being stored within the databse. 

---

## 8. Appendix 

Use your team repo to version control additional resources. Link to them here.
repository: https://bitbucket.org/sml3/cse216_sp26_team_04/src/main/ 
hosted application: https://team-04.dokku.cse.lehigh.edu/ 
Jira: https://lehigh-team-j3itikgk.atlassian.net/jira/software/projects/TEAM4/boards/2 

1. What was done well, and should be documented so it can be repeated in the future?
- The team successfully divided responsibilities and completed all major components
- Communication improved over time
- Testing and debugging were effective in later phases

2. What could have been done better? And if so, how could it have been done better? 
- Better time management earlier in the project
- More consistent testing throughout development

3. What was the most important lesson you learned in cse216?
- Learned how to build and maintain a full-stack system, including database design, testing, and deployment
- learned how to work well as a team, communicating with each other and code with each other.

## 9 Gen AI (everyone needs to answer this one)
Capturing the use of generative AI is important for provenance and attribution. At its core, this section should cover how each team member as well as the team overall used genAI in a general sense when doing the project-based work reflected in the repo, as well as a per-component breakdown of where/how genAI was used. Recall that you are/were expected to cite or otherwise provide attribution – just as you would for any work that is not your own. 

- Team- Overall, our team used generative AI to help guide understanding, help debug errors, and help with creating broad unit tests. Places where it was used primarily are marked with comments attributing the methods or classes to the specific ai model that created it.  

- Sakshi Mishra- I used generative AI primarily to help generate tests and to help go over concepts that I wasn't familiar with when programming certain parts of the document. I wanted to make sure I fully understood what I was doing while coding so to double check concepts or understand errors that I wasn't sure how to read, I would use genAI to help me find a solution while still making sure I learned how to accomplish the task. Additionally, I used it to create basic unit tests in order to get an idea of what would need to be tested the most for certain parts of the program. Anything I didn't code on my own and used gen AI for is marked with a comment at the start to make it perfectly clear. Specifically, I used genAI for tests in the admin component. 

- Leo Xu - I used generative AI to help debug SQL and Java issues, also use it to get understanding from the generative AI. I also used it to assist with writing Javadoc comments, improving unit tests, and refactoring code. All generated content was reviewed, tested, and modified before being used in the final implementation.

- Jack Lamb - Throughout the project I used AI to help assist with debugging. When I would run ssh dokku logs, it would output 100+ lines so being able to pinpoint the exact errors with AI was super helpful. Instead of wasting time reading 100+ lines of code, it made the process much more efficient. Claude would also help explain what the error meant and common fixes, which helped lead a path forward on how to fix the issues. Additionally, when using inspect element on chrome the console would show dozens of requests that I wasn’t sure how to read, and AI helped in a similar way as above and helped pinpoint the issues and explain what was going wrong.

- Julia Salinero - My main uses of generative AI were for debugging and testing purposes. It became especially helpful when in crunch time on Zoom, the night a sprint was due, trying to fix last minute bugs while still making sure I learned how to accomplish the task. I also used it to create frontend tests during Phase 4, which I stated directly in that file. Apart from that, I found AI useful at the beginning of each phase. I would provide the phase specs and it would help me divide the tasks up more clearly, in addition to keeping up with the Jira board. I found this useful because I was able to keep my own lists of tasks, as I do for other classes, and stay organized to complete everything on time. Lastly, I used generative AI as a second set of eyes to catch inconsistencies across files and clean up any typos before submitting. In these cases, I always reviewed and understood the suggested changes before applying them.